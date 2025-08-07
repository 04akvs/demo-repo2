package com.db.tflm.sdpingestion.util;

import com.db.tf.messaging.config.Topic;
import com.db.tf.messaging.producer.Producer;
import com.db.tflm.sdpingestion.SpringApplicationContext;
import com.db.tflm.sdpingestion.simulation.FacilityEventFeeder;
import com.db.tflm.sdpingestion.simulation.InstrumentEventFeeder;
import com.db.tflm.sdpingestion.simulation.ScenarioConstants;
import com.db.tflm.sdpingestion.test.MockInternalMessageReceiver;
import com.db.tradesense.kannon.Instrument;
import com.db.tradesense.kannon.InstrumentEventType;
import com.db.tradesense.tflm.Facility;
import com.db.tradesense.tflm.FacilityEventType;
import com.fasterxml.jackson.core.type.TypeReference;
import io.gatling.javaapi.core.ChainBuilder;

import jakarta.jms.ConnectionFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static io.gatling.javaapi.core.CoreDsl.*;
import static com.db.tflm.sdpingestion.util.KafkaJmsActionBuilder.*;

public class RequestUtil {
    
    // Spring beans - initialized lazily
    private static Topic instrumentTopic;
    private static Topic facilityTopic;
    private static Producer producer;
    private static ConnectionFactory connectionFactory;
    private static MockInternalMessageReceiver internalMessageReceiver;
    
    private static final AtomicInteger counter = new AtomicInteger(0);
    
    // Initialize Spring beans
    static {
        try {
            SpringApplicationContext ctx = SpringApplicationContext.getInstance();
            instrumentTopic = ctx.getApplicationContext().getBeansOfType(Topic.class).get("instrumentTopic");
            facilityTopic = ctx.getApplicationContext().getBeansOfType(Topic.class).get("facilityTopic");
            producer = ctx.getApplicationContext().getBean(Producer.class);
            connectionFactory = ctx.getApplicationContext().getBean(ConnectionFactory.class);
            internalMessageReceiver = ctx.getApplicationContext().getBean(MockInternalMessageReceiver.class);
        } catch (Exception e) {
            System.err.println("Failed to initialize Spring beans: " + e.getMessage());
            // Beans will be null and should be checked before use
        }
    }
    
    /**
     * Generates unique Kafka message keys
     */
    public static String getKafkaMessageKey() {
        return "EB74E14FFED9116-00000" + counter.incrementAndGet();
    }
    
    /**
     * Sends instrument to Kafka and receives response in internal queue
     */
    public static ChainBuilder sendInstrumentToKafkaAndReceiveInInternalQueue(
            InstrumentEventType eventType, 
            int repeatNum, 
            String filePath, 
            Consumer<Instrument> beforeSendingMessage) {
        
        InstrumentEventFeeder eventFeeder = new InstrumentEventFeeder(eventType);
        
        return repeat(repeatNum).on(
            feed(eventFeeder.get())
            .exec(ElFileBody(filePath))  // This resolves ${eventType} and other EL expressions
            .exec(KafkaJmsActionBuilder.kafkaJmsAction(
                ScenarioConstants.SEND_INSTRUMENT_REQUEST_TO_KAFKA_RECEIVE_IN_JMS,
                producer,
                instrumentTopic,
                session -> getKafkaMessageKey(),
                internalMessageReceiver,
                new TypeReference<Instrument>() {},
                instrument -> ((Instrument) instrument).getExposure().getEventId(),
                beforeSendingMessage != null ? obj -> beforeSendingMessage.accept((Instrument) obj) : null
            ))
        );
    }
    
    /**
     * Overloaded method with default parameters
     */
    public static ChainBuilder sendInstrumentToKafkaAndReceiveInInternalQueue(
            InstrumentEventType eventType, 
            String filePath, 
            Consumer<Instrument> beforeSendingMessage) {
        return sendInstrumentToKafkaAndReceiveInInternalQueue(
            eventType, 
            ScenarioConstants.NUM_TRANSACTIONS, 
            filePath, 
            beforeSendingMessage
        );
    }
    
    /**
     * Sends facility to Kafka and receives response in internal queue
     */
    public static ChainBuilder sendFacilityToKafkaAndReceiveInInternalQueue(
            FacilityEventType eventType, 
            int repeatNum, 
            String filePath, 
            Consumer<Facility> beforeSendingMessage) {
        
        FacilityEventFeeder eventFeeder = new FacilityEventFeeder(eventType);
        
        return repeat(repeatNum).on(
            feed(eventFeeder.get())
            .exec(ElFileBody(filePath))  // This resolves ${eventType} and other EL expressions
            .exec(KafkaJmsActionBuilder.kafkaJmsAction(
                ScenarioConstants.SEND_FACILITY_REQUEST_TO_KAFKA_RECEIVE_IN_JMS,
                producer,
                facilityTopic,
                session -> getKafkaMessageKey(),
                internalMessageReceiver,
                new TypeReference<Facility>() {},
                facility -> ((Facility) facility).getExposure().getEventId(),
                beforeSendingMessage != null ? obj -> beforeSendingMessage.accept((Facility) obj) : null
            ))
        );
    }
    
    /**
     * Overloaded method with default parameters
     */
    public static ChainBuilder sendFacilityToKafkaAndReceiveInInternalQueue(
            FacilityEventType eventType, 
            String filePath, 
            Consumer<Facility> beforeSendingMessage) {
        return sendFacilityToKafkaAndReceiveInInternalQueue(
            eventType, 
            ScenarioConstants.NUM_TRANSACTIONS, 
            filePath, 
            beforeSendingMessage
        );
    }
    
    // Getters for Spring beans (for testing or manual access)
    public static Topic getInstrumentTopic() { return instrumentTopic; }
    public static Topic getFacilityTopic() { return facilityTopic; }
    public static Producer getProducer() { return producer; }
    public static ConnectionFactory getConnectionFactory() { return connectionFactory; }
    public static MockInternalMessageReceiver getInternalMessageReceiver() { return internalMessageReceiver; }
}