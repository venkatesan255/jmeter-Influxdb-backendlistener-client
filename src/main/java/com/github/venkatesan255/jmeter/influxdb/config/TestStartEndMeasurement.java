package com.github.venkatesan255.jmeter.influxdb.config;

/* Constants (Tag, Field, Measurement) names for the measurement that denotes start and end points of a load test. */

public interface TestStartEndMeasurement {

    String MEASUREMENT_NAME = "testStartEnd";

    interface Tags {
        String TYPE = "type";
        String NODE_NAME = "nodeName";
        String RUN_ID = "runId";
        String TEST_NAME = "testName";
    }

    interface Fields {
        String PLACEHOLDER = "placeholder";
    }

    interface Values {
        String FINISHED = "finished";
        String STARTED = "started";
    }


}
