package com.db.tflm.sdpingestion.simulation;

public class ScenarioConstants {
    
    // Request names
    public static final String SEND_INSTRUMENT_REQUEST_TO_KAFKA_RECEIVE_IN_JMS = "Send instrument request to Kafka and receive in JMS";
    public static final String SEND_FACILITY_REQUEST_TO_KAFKA_RECEIVE_IN_JMS = "Send facility request to Kafka and receive in JMS";
    
    // Transaction numbers
    public static final int NUM_TRANSACTIONS = 100;
    public static final int WARMUP_NUM_TRANSACTIONS = 10;
    
    // Performance assertions
    public static final double SUCCESSFUL_REQUESTS_PERCENTAGE = 95.0;
    public static final int MAX_RESPONSE_TIME = 5000; // milliseconds
    public static final int MEAN_RESPONSE_TIME = 2000; // milliseconds
    
    private ScenarioConstants() {
        // Utility class - prevent instantiation
    }
}