package com.jmeter.influxdb.config;

/* Constants (Tag, Field, Measurement) names for the requests measurement. */

public interface RequestMeasurement {

    String MEASUREMENT_NAME = "requestsRaw";

    interface Tags {
        String REQUEST_NAME = "requestName";
        String RUN_ID = "runId";
        String TEST_NAME = "testName";

    }

    interface Fields {
        String RESPONSE_TIME = "responseTime";
        String ERROR_COUNT = "errorCount";
        String THREAD_NAME = "threadName";
        String NODE_NAME = "nodeName";
        String RESPONSE_MSG = "responseMsg";
        String RESPONSE_CODE = "responseCode";
        String ASSERTION_MSG = "assertionMsg";
        String REQ_HEADER = "requestHeader";
        String RES_HEADER = "responseHeader";
        String REQ_PAYLOAD = "reqPayload";
        String RES_DATA = "responseData";
    }

}
