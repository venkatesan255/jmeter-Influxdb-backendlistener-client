package com.jmeter.influxdb.config;

/* Constants (Tag, Field, Measurement) names for the virtual users measurement. */

public interface VirtualUsersMeasurement {
    String MEASUREMENT_NAME = "virtualUsers";

    interface Tags {

        String NODE_NAME = "nodeName";
        String TEST_NAME = "testName";
        String RUN_ID = "runId";
    }

    interface Fields {

        String MIN_ACTIVE_THREADS = "minActiveThreads";
        String MAX_ACTIVE_THREADS = "maxActiveThreads";
        String MEAN_ACTIVE_THREADS = "meanActiveThreads";
        String STARTED_THREADS = "startedThreads";
        String FINISHED_THREADS = "finishedThreads";
    }

}
