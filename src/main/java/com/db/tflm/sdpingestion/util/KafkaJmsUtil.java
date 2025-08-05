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
     * Simplified method that automatically gets message body from session (set by ElFileBody)
     */
    public static Function<Session, Session> sendToKafkaAndReceiveFromJms(
            String requestName,
            Producer kafkaProducer,
            Topic topic,
            Function<Session, String> messageKeyFunc,
            MockInternalMessageReceiver jmsReceiver,
            TypeReference<?> typeRef,
            Function<Object, NonVersionedIdentifier> messageMatcherFunction,
            Consumer<Object> beforeSendCallback) {
        
        return session -> {
            String messageKey = messageKeyFunc.apply(session);
            // Get the message body from session (resolved by ElFileBody)
            String messageBody = getMessageBodyFromSession(session);
            
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
            // Get the message body from session (resolved by ElFileBody)
            String messageBody = getMessageBodyFromSession(session);
            
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
     * Gets the message body from session with proper EL resolution
     * This handles both ElFileBody content and manual body setting
     */
    private static String getMessageBodyFromSession(Session session) {
        // First try the standard Gatling session key for ElFileBody
        String body = session.getString("gatling.core.body.string");
        
        if (body == null) {
            // Fallback to custom key if set manually
            body = session.getString("requestBody");
        }
        
        if (body == null) {
            throw new RuntimeException("No message body found in session. Make sure to call ElFileBody() before this action.");
        }
        
        // The body should already be resolved by Gatling's ElFileBody
        return body;
    }
}