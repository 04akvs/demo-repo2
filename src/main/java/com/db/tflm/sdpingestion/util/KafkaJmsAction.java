package com.db.tflm.sdpingestion.util;

import com.db.sdp.common.NonVersionedIdentifier;
import com.db.tf.messaging.config.Topic;
import com.db.tf.messaging.core.TfMessage;
import com.db.tf.messaging.producer.Producer;
import com.db.tflm.sdpingestion.test.MockInternalMessageReceiver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.commons.stats.Status;
import io.gatling.core.action.Action;
import io.gatling.core.session.Session;
import io.gatling.core.stats.StatsEngine;
import io.gatling.core.util.NameGen;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Simple custom action that properly integrates with Gatling's stats engine
 */
public class KafkaJmsAction implements Action, NameGen {
    
    private final String requestName;
    private final Producer kafkaProducer;
    private final Topic topic;
    private final Function<Session, String> messageKeyFunc;
    private final MockInternalMessageReceiver jmsReceiver;
    private final TypeReference<?> typeRef;
    private final Function<Object, NonVersionedIdentifier> messageMatcherFunction;
    private final Consumer<Object> beforeSendCallback;
    private final Action next;
    private final StatsEngine statsEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KafkaJmsAction(
            String requestName,
            Producer kafkaProducer,
            Topic topic,
            Function<Session, String> messageKeyFunc,
            MockInternalMessageReceiver jmsReceiver,
            TypeReference<?> typeRef,
            Function<Object, NonVersionedIdentifier> messageMatcherFunction,
            Consumer<Object> beforeSendCallback,
            Action next,
            StatsEngine statsEngine) {
        
        this.requestName = requestName;
        this.kafkaProducer = kafkaProducer;
        this.topic = topic;
        this.messageKeyFunc = messageKeyFunc;
        this.jmsReceiver = jmsReceiver;
        this.typeRef = typeRef;
        this.messageMatcherFunction = messageMatcherFunction;
        this.beforeSendCallback = beforeSendCallback;
        this.next = next;
        this.statsEngine = statsEngine;
    }

    @Override
    public String name() {
        return genName("kafkaJmsAction");
    }

    @Override
    public void execute(Session session) {
        long startTime = System.currentTimeMillis();
        String resolvedRequestName = requestName;
        
        try {
            // Get message key
            String messageKey = messageKeyFunc.apply(session);
            
            // Get message body from session (set by ElFileBody)
            String messageBody = getMessageBodyFromSession(session);
            
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
            
            // Wait for JMS response with timeout
            jmsReceiver.get(eventId, 5000).get(); // 5 second timeout
            
            long endTime = System.currentTimeMillis();
            
            // Log successful response to Gatling stats
            statsEngine.logResponse(
                session.scenario(),
                session.groups(),
                resolvedRequestName,
                startTime,
                endTime,
                Status.apply("OK"),
                scala.Option.apply("200"),
                scala.Option.apply("Message with id=" + eventId.getId() + " successfully sent and received")
            );
            
            System.out.println(String.format(
                "[SUCCESS] %s - Message %s processed in %d ms", 
                resolvedRequestName, eventId.getId(), (endTime - startTime)
            ));
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            
            // Log failure to Gatling stats
            statsEngine.logResponse(
                session.scenario(),
                session.groups(),
                resolvedRequestName,
                startTime,
                endTime,
                Status.apply("KO"),
                scala.Option.apply("500"),
                scala.Option.apply(e.getMessage())
            );
            
            System.err.println(String.format(
                "[FAILURE] %s - Error: %s (took %d ms)", 
                resolvedRequestName, e.getMessage(), (endTime - startTime)
            ));
        } finally {
            // ALWAYS continue with next action to prevent hanging
            next.execute(session);
        }
    }
    
    private String getMessageBodyFromSession(Session session) {
        // First try the standard Gatling session key for ElFileBody
        String body = session.getString("gatling.core.body.string");
        
        if (body == null) {
            // Fallback to custom key if set manually
            body = session.getString("requestBody");
        }
        
        if (body == null) {
            throw new RuntimeException("No message body found in session. Make sure to call ElFileBody() before this action.");
        }
        
        return body;
    }
}