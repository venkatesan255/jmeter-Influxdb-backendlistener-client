package com.github.venkatesan255.jmeter.influxdb.client;

import com.github.venkatesan255.jmeter.influxdb.config.InfluxDBConfig;
import com.github.venkatesan255.jmeter.influxdb.config.RequestMeasurement;
import com.github.venkatesan255.jmeter.influxdb.config.TestStartEndMeasurement;
import com.github.venkatesan255.jmeter.influxdb.config.VirtualUsersMeasurement;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JMeterInfluxDBBackendListenerClient extends AbstractBackendListenerClient implements Runnable {

    //    private static final Logger LOGGER = LoggingManager.getLoggerForClass();
    private static final Logger LOGGER = LoggerFactory.getLogger(JMeterInfluxDBBackendListenerClient.class);

    private static final String KEY_USE_REGEX_FOR_SAMPLER_LIST = "UseRegexForSamplerList";
    private static final String KEY_TEST_NAME = "TestName";
    private static final String KEY_RUN_ID = "RunId";
    private static final String KEY_NODE_NAME = "NodeName";
    private static final String KEY_SAMPLERS_LIST = "SamplersList";
    private static final String KEY_RECORD_SUB_SAMPLES = "RecordSubSamples";
    private static final String KEY_TAGS_LISTING = "OptionalTags";
    private static final String DEFAULT_RESPONSE_DATA = "NULL";
    private static final String DEFAULT_SAMPLER_DATA = "NULL";
    private static final String DEFAULT_REQ_HEADER = "NULL";
    private static final String DEFAULT_RES_HEADER = "NULL";


    /* Constants */

    private static final String SEPARATOR = ";";
    private static final int ONE_MS_IN_NANOSECONDS = 1000000;

    /* Scheduler for periodic metric aggregation */

    private ScheduledExecutorService scheduler;

    private String testName;
    private String runId;
    private String nodeName;
    private String regexForSamplerList;
    private Set<String> samplersToFilter;

    /*
     * for storing optionalTags to InfluxDB. Each tag entry is a key value par [TAG_KEY] = [TAG_VALUE]
     * delimiters allowed comma (,) colon(:) or semicolon (;)
     * */
    private String tagsListing;

    InfluxDBConfig influxDBConfig;
    private InfluxDB influxDB;
    private Random randomNumberGenerator;
    private boolean recordSubSamples;

    /* Backend listener default parameters */
    @Override
    public Arguments getDefaultParameters() {

        Arguments arguments = new Arguments();
        arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_HOST, InfluxDBConfig.DEFAULT_HOST);
        arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_PORT, Integer.toString(InfluxDBConfig.DEFAULT_PORT));
        arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_DATABASE, InfluxDBConfig.DEFAULT_DATABASE);
        arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_USER, "user");
        arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_PASSWORD, "password");
        arguments.addArgument(InfluxDBConfig.KEY_RETENTION_POLICY, InfluxDBConfig.DEFAULT_RETENTION_POLICY);


        arguments.addArgument(KEY_NODE_NAME, "Test-Node");
        arguments.addArgument(KEY_TEST_NAME, "Test");
        arguments.addArgument(KEY_RUN_ID, "R0001");
        arguments.addArgument(KEY_USE_REGEX_FOR_SAMPLER_LIST, "true");
        arguments.addArgument(KEY_SAMPLERS_LIST, ".*");
        arguments.addArgument(KEY_RECORD_SUB_SAMPLES, "false");
        arguments.addArgument(KEY_TAGS_LISTING, "");


        return arguments;

    }

    /* Setup InfluxDB connection */
    private void setupInfluxClient(BackendListenerContext context) {
        influxDBConfig = new InfluxDBConfig(context);
        influxDB = InfluxDBFactory.connect(influxDBConfig.getInfluxDBURL(), influxDBConfig.getInfluxUser(), influxDBConfig.getInfluxPassword());
        influxDB.enableBatch(100, 5, TimeUnit.SECONDS);
        createDatabaseIfNotExistent();

    }

    /* Creates the configured database in influx if it does not exist yet */
    private void createDatabaseIfNotExistent() {
        List<String> dbNames = influxDB.describeDatabases();
        if (!dbNames.contains(influxDBConfig.getInfluxDatabase())) {
            influxDB.createDatabase(influxDBConfig.getInfluxDatabase());
        }
    }

    /* Parses list of samplers. */
    private void parseSamplers(BackendListenerContext context) {
        String samplersList = context.getParameter(KEY_SAMPLERS_LIST, "");
        samplersToFilter = new HashSet<String>();
        if (context.getBooleanParameter(KEY_USE_REGEX_FOR_SAMPLER_LIST, false)) {
            regexForSamplerList = samplersList;
        } else {
            regexForSamplerList = null;
            String[] samplers = samplersList.split(SEPARATOR);
            samplersToFilter.addAll(Arrays.asList(samplers));
        }
    }

    @Override
    public void setupTest(BackendListenerContext context) {

        testName = context.getParameter(KEY_TEST_NAME, "Test");
        runId = context.getParameter(KEY_RUN_ID, "R0001");
        nodeName = context.getParameter(KEY_NODE_NAME, "Test-Node");
        randomNumberGenerator = new Random();
        tagsListing = context.getParameter(KEY_TAGS_LISTING);


        /* initialise Influx connection */
        setupInfluxClient(context);

        /* Samples filter check */
        parseSamplers(context);

        /* Create point to add metrics to Influx tables */
        Point point = Point.measurement(TestStartEndMeasurement.MEASUREMENT_NAME)
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .tag(TestStartEndMeasurement.Tags.NODE_NAME, nodeName)
                .tag(TestStartEndMeasurement.Tags.TEST_NAME, testName)
                .tag(TestStartEndMeasurement.Tags.RUN_ID, runId)
                .tag(TestStartEndMeasurement.Tags.TYPE, TestStartEndMeasurement.Values.STARTED)
                .addField(TestStartEndMeasurement.Fields.PLACEHOLDER, "1")
                .build();

        /* Write data to Influx table */
        influxDB.write(influxDBConfig.getInfluxDatabase(), influxDBConfig.getInfluxRetentionPolicy(), point);

        /* Polling settings */
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);

        // Indicates whether to write sub sample records to the database
        recordSubSamples = Boolean.parseBoolean(context.getParameter(KEY_RECORD_SUB_SAMPLES, "false"));

    }

    public void teardownTest(BackendListenerContext context) throws Exception {
        LOGGER.info("Shutting down influxDB scheduler...");
        scheduler.shutdown();

        addVirtualUsersMetrics(0, 0, 0, 0, JMeterContextService.getThreadCounts().finishedThreads);


        /* Create point to add metrics to Influx tables */
        Point point = Point.measurement(TestStartEndMeasurement.MEASUREMENT_NAME)
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .tag(TestStartEndMeasurement.Tags.NODE_NAME, nodeName)
                .tag(TestStartEndMeasurement.Tags.TEST_NAME, testName)
                .tag(TestStartEndMeasurement.Tags.RUN_ID, runId)
                .tag(TestStartEndMeasurement.Tags.TYPE, TestStartEndMeasurement.Values.FINISHED)
                .addField(TestStartEndMeasurement.Fields.PLACEHOLDER, "1")
                .build();

        /* Write data to Influx table */
        influxDB.write(influxDBConfig.getInfluxDatabase(), influxDBConfig.getInfluxRetentionPolicy(), point);

        influxDB.disableBatch();

        try {
            scheduler.awaitTermination(30, TimeUnit.SECONDS);
            LOGGER.info("InfluxDB scheduler terminated!");
        } catch (InterruptedException e) {
            LOGGER.error("Error waiting for end of scheduler");
        }

        samplersToFilter.clear();
        super.teardownTest(context);


    }





    /* Processes sampler results. */
    public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {


        String reqHeader = "";
        String resHeader = "";
        String responseMsg = "";
        StringBuilder resData = new StringBuilder();
        String reqPayload = "";

        /* Gather all the listeners */
        List<SampleResult> allSampleResults = new ArrayList<SampleResult>();

        /* Loop through all the samplers */
        for (SampleResult sampleResult : sampleResults) {
            allSampleResults.add(sampleResult);
            /* if recordSubSamples enabled, send metrics for subSamplers as well */
            if (recordSubSamples) {
                allSampleResults.addAll(Arrays.asList(sampleResult.getSubResults()));
            }
        }

        for (SampleResult sampleResult : allSampleResults) {
            SampleResult[] transactionSubResults = sampleResult.getSubResults();

            AssertionResult[] assertionResults;

            /* check if the sampler has any sub results. if it has, it means it is a transaction controller. otherwise get user metrics as it is */

            if (transactionSubResults.length > 0) {
                for (SampleResult subResult : transactionSubResults) {
                    if(!subResult.isSuccessful()) {
                        assertionResults = subResult.getAssertionResults();
                        updateSampleResponseMessage(subResult,assertionResults);
                        responseMsg = subResult.getResponseMessage();
                        reqHeader = subResult.getRequestHeaders();
                        resHeader = subResult.getResponseHeaders();
                        reqPayload = subResult.getSamplerData();
                        resData.append(subResult.getResponseDataAsString());
                    }
                }

                sampleResult.setResponseMessage(responseMsg);
                sampleResult.setRequestHeaders(reqHeader);
                sampleResult.setResponseHeaders(resHeader);
                sampleResult.setSamplerData(reqPayload);
                sampleResult.setResponseData(String.valueOf(resData),SampleResult.DEFAULT_HTTP_ENCODING);

            }





            getUserMetrics().add(sampleResult);
            try {

                if (
                        (null != regexForSamplerList && sampleResult.getSampleLabel().matches(regexForSamplerList)) ||
                                samplersToFilter.contains(sampleResult.getSampleLabel())
                ) {

                    Map<String, String> tags = processOptionalTags(tagsListing);
                    Point.Builder point = Point.measurement(RequestMeasurement.MEASUREMENT_NAME)
                            .time(System.currentTimeMillis() * ONE_MS_IN_NANOSECONDS + getUniqueNumberForTheSamplerThread(), TimeUnit.NANOSECONDS)
                            .tag(RequestMeasurement.Tags.RUN_ID, runId)
                            .tag(RequestMeasurement.Tags.TEST_NAME, testName)
                            .tag(RequestMeasurement.Tags.REQUEST_NAME, sampleResult.getSampleLabel())
                            .addField(RequestMeasurement.Fields.THREAD_NAME, sampleResult.getThreadName())
                            .addField(RequestMeasurement.Fields.NODE_NAME, nodeName)
                            .addField(RequestMeasurement.Fields.ERROR_COUNT, sampleResult.getErrorCount())
                            .addField(RequestMeasurement.Fields.RESPONSE_TIME, sampleResult.getTime())
                            .addField(RequestMeasurement.Fields.RESPONSE_MSG, sampleResult.getResponseMessage())
                            .addField(RequestMeasurement.Fields.RESPONSE_CODE, sampleResult.getResponseCode());


                    if (!sampleResult.isSuccessful()) {

                        if (StringUtils.isEmpty(sampleResult.getResponseDataAsString())) {
                            sampleResult.setResponseData(DEFAULT_RESPONSE_DATA, SampleResult.DEFAULT_HTTP_ENCODING);
                        }
                        if (StringUtils.isEmpty(sampleResult.getRequestHeaders())) {
                            sampleResult.setRequestHeaders(DEFAULT_REQ_HEADER);
                        }
                        if (StringUtils.isEmpty(sampleResult.getResponseHeaders())) {
                            sampleResult.setResponseHeaders(DEFAULT_RES_HEADER);
                        }
                        if (StringUtils.isEmpty(sampleResult.getSamplerData())) {
                            sampleResult.setSamplerData(DEFAULT_SAMPLER_DATA);
                        }

                        point.addField(RequestMeasurement.Fields.REQ_HEADER, sampleResult.getRequestHeaders())
                                .addField(RequestMeasurement.Fields.RES_HEADER, sampleResult.getResponseHeaders())
                                .addField(RequestMeasurement.Fields.REQ_PAYLOAD, sampleResult.getSamplerData())
                                .addField(RequestMeasurement.Fields.RES_DATA, sampleResult.getResponseDataAsString());
                    }

                    addOptionalTagsToPoint(point, tags);

                    influxDB.write(influxDBConfig.getInfluxDatabase(), influxDBConfig.getInfluxRetentionPolicy(), point.build());

                }

            }catch (Exception e ) {
                    LOGGER.info("error in writing data to influxDB " + e);
            }

        }

    }


    private String addAssertionResults(AssertionResult[] assertionResults) {
        StringBuilder failureMsg = new StringBuilder();
        if (assertionResults != null) {
            for (AssertionResult assertionResult : assertionResults) {
                failureMsg
                        .append(assertionResult.getFailureMessage())
                        .append(";\r");
            }
        }
        return failureMsg.toString();
    }

    private void updateSampleResponseMessage(SampleResult sampleResult, AssertionResult[] assertions) {

        String  responseMsgTemp = "";

        responseMsgTemp += sampleResult.getResponseMessage() ;

        if(assertions.length > 0) {
            responseMsgTemp += "; \rNumber of failed assertions: " + assertions.length + "\r"
                    + generateAssertionTrace(assertions);
        }

        sampleResult.setResponseMessage(responseMsgTemp);


    }

    private String generateAssertionTrace(AssertionResult[] assertions) {
        StringBuilder assertionLogMessage = new StringBuilder();

        if (assertions.length > 0) {
            for (AssertionResult assertion : assertions) {
                assertionLogMessage
                        .append(assertion.getName())
                        .append(" Failed; \r")
                        .append("Failure Message: ")
                        .append(assertion.getFailureMessage())
                        .append(";\r");
            }
        }
        return assertionLogMessage.toString();
    }

    /**
     * Splits a passed string to key-value pairs whereas a delimited by coma, colon or semicolon.
     * Whereas key is a tag to be recorded in the InfluxDB-database and value is its value.
     * For ex. "appVersion=4.1.11;testdataVersion=3.0" means that the InfluxDB gets two tags "appVersion" and "testdataVersion"
     * with values.
     * <p>
     * param listOfOptionalTags
     * return a map object of [tag]-[value] pairs.
     */

    private Map<String, String> processOptionalTags(String listOfOptionalTags) {

        final String tagPairsDelimiterRegex = "[,;:]";
        final String keyValueDelimiterRegex = "=";

        String[] keyValuePairs = listOfOptionalTags.split(tagPairsDelimiterRegex);

        Map<String, String> result = new HashMap<String, String>();

        if (StringUtils.isEmpty(listOfOptionalTags))
            return result;

        for (String pair : keyValuePairs) {
            String[] singleTag = pair.split(keyValueDelimiterRegex);
            if (singleTag.length == 2)
                result.put(singleTag[0].trim(), singleTag[1].trim());
        }
        return result;


    }

    /**
     * If the argument "optionalTags" set in the JMeter-GUI contains some valid tag data then this data gets added
     * to the current measurement point.
     *
     * @param pointBuilder a measurement point object.
     * @param tags         a map of [tag]-[value] pairs.
     */
    private void addOptionalTagsToPoint(Point.Builder pointBuilder, Map<String, String> tags) {

        for (Map.Entry<String, String> tempMap : tags.entrySet()) {
            pointBuilder.tag(tempMap.getKey(), tempMap.getValue());
        }

    }

    private void addVirtualUsersMetrics(int minActiveThreads, int meanActiveThreads, int maxActiveThreads, int startedThreads, int finishedThreads) {
        Point.Builder builder = Point.measurement(VirtualUsersMeasurement.MEASUREMENT_NAME).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        builder.addField(VirtualUsersMeasurement.Tags.NODE_NAME, nodeName);
        builder.addField(VirtualUsersMeasurement.Tags.RUN_ID, runId);
        builder.addField(VirtualUsersMeasurement.Tags.TEST_NAME, testName);
        builder.addField(VirtualUsersMeasurement.Fields.STARTED_THREADS, startedThreads);
        builder.addField(VirtualUsersMeasurement.Fields.FINISHED_THREADS, finishedThreads);
        builder.addField(VirtualUsersMeasurement.Fields.MIN_ACTIVE_THREADS, minActiveThreads);
        builder.addField(VirtualUsersMeasurement.Fields.MAX_ACTIVE_THREADS, maxActiveThreads);
        builder.addField(VirtualUsersMeasurement.Fields.MEAN_ACTIVE_THREADS, meanActiveThreads);
        influxDB.write(influxDBConfig.getInfluxDatabase(), influxDBConfig.getInfluxRetentionPolicy(), builder.build());

    }


    /* to get a unique number for the sampler thread */
    private int getUniqueNumberForTheSamplerThread() {
        return randomNumberGenerator.nextInt(ONE_MS_IN_NANOSECONDS);
    }


    public void run() {
        try {
            addVirtualUsersMetrics(getUserMetrics().getMinActiveThreads()
                    , getUserMetrics().getMeanActiveThreads()
                    , getUserMetrics().getMaxActiveThreads()
                    , getUserMetrics().getStartedThreads()
                    , getUserMetrics().getFinishedThreads()
            );

        } catch (Exception e) {
            LOGGER.error("Failed writing to influx ", e);
        }

    }
}