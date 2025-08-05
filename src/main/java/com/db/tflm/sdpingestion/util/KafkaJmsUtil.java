package com.db.tflm.sdpingestion.util;

import com.db.sdp.common.NonVersionedIdentifier;
import com.db.tf.messaging.config.Topic;
import com.db.tf.messaging.core.TfMessage;
import com.db.tf.messaging.producer.Producer;
import com.db.tflm.sdpingestion.test.MockInternalMessageReceiver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.Session;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class for Kafka/JMS operations using standard Gatling DSL
 */
public class KafkaJmsUtil {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Sends a message to Kafka and waits for JMS response
     * This is a standard session function that can be used with exec()
     */
    public static Function<Session, Session> sendToKafkaAndReceiveFromJms(
            String requestName,
            Producer kafkaProducer,
            Topic topic,
            String messageKey,
            String messageBody,
            MockInternalMessageReceiver jmsReceiver,
            TypeReference<?> typeRef,
            Function<Object, NonVersionedIdentifier> messageMatcherFunction,
            Consumer<Object> beforeSendCallback) {
        
        return session -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // Parse the message
                Object message = objectMapper.readValue(messageBody, typeRef);
                
                // Execute before-send callback
                if (beforeSendCallback != null) {
                    beforeSendCallback.accept(message);
                }
                
                // Get the event ID for matching
                NonVersionedIdentifier eventId = messageMatcherFunction.apply(message);
                
                // Send to Kafka
                TfMessage<?> tfMessage = new TfMessage<>(message, messageKey);
                kafkaProducer.send(tfMessage, topic, messageKey).get();
                
                // Wait for JMS response
                jmsReceiver.get(eventId).get();
                
                long endTime = System.currentTimeMillis();
                
                // Log success
                System.out.println(String.format(
                    "[SUCCESS] %s - Message %s processed in %d ms", 
                    requestName, eventId.getId(), (endTime - startTime)
                ));
                
                // Store timing in session
                return session.set("responseTime", endTime - startTime)
                             .set("status", "OK")
                             .set("message", "Message successfully sent and received");
                
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                
                // Log failure
                System.err.println(String.format(
                    "[FAILURE] %s - Error: %s (took %d ms)", 
                    requestName, e.getMessage(), (endTime - startTime)
                ));
                
                // Store error in session
                return session.set("responseTime", endTime - startTime)
                             .set("status", "KO")
                             .set("error", e.getMessage());
            }
        };
    }
    
    /**
     * Overloaded method with String message key
     */
    public static Function<Session, Session> sendToKafkaAndReceiveFromJms(
            String requestName,
            Producer kafkaProducer,
            Topic topic,
            Function<Session, String> messageKeyFunc,
            Function<Session, String> messageBodyFunc,
            MockInternalMessageReceiver jmsReceiver,
            TypeReference<?> typeRef,
            Function<Object, NonVersionedIdentifier> messageMatcherFunction,
            Consumer<Object> beforeSendCallback) {
        
        return session -> {
            String messageKey = messageKeyFunc.apply(session);
            String messageBody = messageBodyFunc.apply(session);
            
            return sendToKafkaAndReceiveFromJms(
                requestName, kafkaProducer, topic, messageKey, messageBody, 
                jmsReceiver, typeRef, messageMatcherFunction, beforeSendCallback
            ).apply(session);
        };
    }
    
    /**
     * Simple version for cases where the message body comes from ElFileBody
     */
    public static Function<Session, Session> sendInstrumentToKafkaAndReceiveFromJms(
            String requestName,
            Producer kafkaProducer, 
            Topic topic,
            String messageKey,
            MockInternalMessageReceiver jmsReceiver,
            Consumer<Object> beforeSendCallback) {
        
        return session -> {
            // Get the message body from session (set by ElFileBody)
            String messageBody = session.getString("requestBody");
            
            return sendToKafkaAndReceiveFromJms(
                requestName,
                kafkaProducer,
                topic, 
                messageKey,
                messageBody,
                jmsReceiver,
                new TypeReference<Object>() {}, // Generic type
                message -> {
                    // Extract event ID - this would need to be adapted based on actual message structure
                    try {
                        // Assuming the message has a standard structure
                        return (NonVersionedIdentifier) message.getClass()
                            .getMethod("getExposure").invoke(message)
                            .getClass().getMethod("getEventId").invoke(null);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to extract event ID", e);
                    }
                },
                beforeSendCallback
            ).apply(session);
        };
    }
    
    /**
     * Creates a session function that stores the file body in the session
     * This replaces the need for BodyWithStringExpression
     */
    public static Function<Session, Session> loadMessageBody(String filePath) {
        return session -> {
            try {
                // In a real implementation, you'd read the file
                // For now, this is a placeholder that would load from classpath
                String body = loadFileFromClasspath(filePath);
                return session.set("requestBody", body);
            } catch (Exception e) {
                return session.set("error", "Failed to load message body: " + e.getMessage());
            }
        };
    }
    
    private static String loadFileFromClasspath(String filePath) {
        // Placeholder implementation
        // In reality, you'd use Files.readString() or similar
        return "{}"; // Empty JSON as placeholder
    }
}