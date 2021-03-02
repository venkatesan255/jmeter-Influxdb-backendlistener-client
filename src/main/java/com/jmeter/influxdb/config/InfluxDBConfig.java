package com.jmeter.influxdb.config;

import org.apache.jmeter.visualizers.backend.BackendListenerContext;

public class InfluxDBConfig {

    public static final String DEFAULT_DATABASE = "jmeter";
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_RETENTION_POLICY = "autogen";
    public static final String DEFAULT_HTTP_SCHEME = "http";
    public static final int DEFAULT_PORT = 8086;

    public static final String KEY_INFLUX_DB_HOST = "InfluxDBHost";
    public static final String KEY_INFLUX_DB_DATABASE = "InfluxDBDatabase";
    public static final String KEY_INFLUX_DB_PORT = "InfluxDBPort";
    public static final String KEY_INFLUX_DB_USER = "InfluxDBUserName";
    public static final String KEY_INFLUX_DB_PASSWORD = "InfluxDBPassword";
    public static final String KEY_RETENTION_POLICY = "RetentionPolicy";
    public static final String KEY_HTTP_SCHEME = "http";

    private String influxDBHost;
    private String influxUser;
    private String influxPassword;
    private String influxDatabase;
    private String influxRetentionPolicy;
    private int influxDBPort;
    private String influxHTTPScheme;


    public InfluxDBConfig(BackendListenerContext context) {
        String influxHTTPScheme = context.getParameter(KEY_HTTP_SCHEME,DEFAULT_HTTP_SCHEME);
        String influxDBHost = context.getParameter(KEY_INFLUX_DB_HOST,DEFAULT_HOST);
        int influxDBPort = context.getIntParameter(KEY_INFLUX_DB_PORT,DEFAULT_PORT);
        String influxUser = context.getParameter(KEY_INFLUX_DB_USER);
        String influxPassword = context.getParameter(KEY_INFLUX_DB_PASSWORD);
        String influxDatabase = context.getParameter(KEY_INFLUX_DB_DATABASE,DEFAULT_DATABASE);
        String influxRetentionPolicy = context.getParameter(KEY_RETENTION_POLICY,DEFAULT_RETENTION_POLICY);

        setInfluxHTTPScheme(influxHTTPScheme);
        setInfluxDBHost(influxDBHost);
        setInfluxDBPort(influxDBPort);
        setInfluxDatabase(influxDatabase);
        setInfluxUser(influxUser);
        setInfluxPassword(influxPassword);
        setInfluxRetentionPolicy(influxRetentionPolicy);


    }

    /* Build InfluxDB URL */

    public String getInfluxDBURL() {
        return influxHTTPScheme + "://" + influxDBHost + ":" + influxDBPort;
    }


    public String getInfluxDBHost() {
        return influxDBHost;
    }

    public void setInfluxDBHost(String influxDBHost) {
        this.influxDBHost = influxDBHost;
    }

    public String getInfluxUser() {
        return influxUser;
    }

    public void setInfluxUser(String influxUser) {
        this.influxUser = influxUser;
    }

    public String getInfluxPassword() {
        return influxPassword;
    }

    public void setInfluxPassword(String influxPassword) {
        this.influxPassword = influxPassword;
    }

    public String getInfluxDatabase() {
        return influxDatabase;
    }

    public void setInfluxDatabase(String influxDatabase) {
        this.influxDatabase = influxDatabase;
    }

    public String getInfluxRetentionPolicy() {
        return influxRetentionPolicy;
    }

    public void setInfluxRetentionPolicy(String influxRetentionPolicy) {
        this.influxRetentionPolicy = influxRetentionPolicy;
    }

    public int getInfluxDBPort() {
        return influxDBPort;
    }

    public void setInfluxDBPort(int influxDBPort) {
        this.influxDBPort = influxDBPort;
    }

    public String getInfluxHTTPScheme() {
        return influxHTTPScheme;
    }

    public void setInfluxHTTPScheme(String influxHTTPScheme) {
        this.influxHTTPScheme = influxHTTPScheme;
    }
}
