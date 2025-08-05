package com.db.tflm.sdpingestion.request.actions;

import com.db.sdp.common.NonVersionedIdentifier;
import com.db.tf.messaging.config.Topic;
import com.db.tf.messaging.core.TfMessage;
import com.db.tf.messaging.producer.Producer;
import com.db.tflm.sdpingestion.test.MockInternalMessageReceiver;
import com.fasterxml.jackson.core.type.TypeReference;
import io.gatling.core.action.Action;
import io.gatling.core.session.Session;
import io.gatling.core.structure.ScenarioContext;
import io.gatling.javaapi.core.Body;
import io.gatling.javaapi.core.Session.Expression;
import io.gatling.commons.stats.Status;
import io.gatling.core.stats.StatsEngine;
import io.gatling.core.util.NameGen;
import scala.util.Failure;
import scala.util.Success;
import scala.util.Try;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class KafkaRequestReplyJmsAction<T> implements Action, NameGen {
    
    private final ScenarioContext ctx;
    private final Optional<Producer> kafkaProducer;
    private final Optional<MockInternalMessageReceiver> internalMessageReceiver;
    private final Optional<Topic> topic;
    private final Optional<Body.WithString> message;
    private final Optional<Expression<String>> messageKey;
    private final Optional<Consumer<T>> beforeSendMessage;
    private final Optional<TypeReference<T>> typeRef;
    private final Optional<Function<T, NonVersionedIdentifier>> nonVersionedIdentifierMessageMatcher;
    private final Expression<String> requestName;
    private final Action next;
    private final StatsEngine statsEngine;

    public KafkaRequestReplyJmsAction(
            ScenarioContext ctx,
            Optional<Producer> kafkaProducer,
            Optional<MockInternalMessageReceiver> internalMessageReceiver,
            Optional<Topic> topic,
            Optional<Body.WithString> message,
            Optional<Expression<String>> messageKey,
            Optional<Consumer<T>> beforeSendMessage,
            Optional<TypeReference<T>> typeRef,
            Optional<Function<T, NonVersionedIdentifier>> nonVersionedIdentifierMessageMatcher,
            Expression<String> requestName,
            Action next) {
        
        this.ctx = ctx;
        this.kafkaProducer = kafkaProducer;
        this.internalMessageReceiver = internalMessageReceiver;
        this.topic = topic;
        this.message = message;
        this.messageKey = messageKey;
        this.beforeSendMessage = beforeSendMessage;
        this.typeRef = typeRef;
        this.nonVersionedIdentifierMessageMatcher = nonVersionedIdentifierMessageMatcher;
        this.requestName = requestName;
        this.next = next;
        this.statsEngine = ctx.coreComponents().statsEngine();
    }

    @Override
    public String name() {
        return genName("kafkaRequestReplyJms");
    }

    @Override
    public void execute(Session session) {
        CompletableFuture.runAsync(() -> {
            try {
                // Extract name from session
                String name = requestName.apply(session);
                
                // Extract message key from session
                String msgId = messageKey.map(expr -> expr.apply(session)).orElse(null);
                
                // Extract and parse message from session  
                String messageBody = message.map(body -> body.apply(session)).orElse(null);
                
                // Parse message to object of type T
                T msg = parseMessage(messageBody);
                
                // Execute beforeSendMessage callback if present
                beforeSendMessage.ifPresent(callback -> callback.accept(msg));
                
                // Create match event ID
                NonVersionedIdentifier matchEventId = nonVersionedIdentifierMessageMatcher
                    .map(matcher -> matcher.apply(msg))
                    .orElse(null);

                long startTimestamp = ctx.coreComponents().clock().nowMillis();

                // Send message to Kafka topic
                if (topic.isPresent()) {
                    Producer producer = kafkaProducer.orElseThrow(() -> 
                        new Exception("Kafka producer not specified"));
                    
                    TfMessage<T> tfMessage = new TfMessage<>(msg, msgId);
                    producer.send(tfMessage, topic.get(), msgId).get();
                }

                // Receive message from JMS queue
                if (internalMessageReceiver.isPresent() && matchEventId != null) {
                    internalMessageReceiver.get().get(matchEventId);
                }

                // Log successful response
                statsEngine.logResponse(
                    session.scenario(),
                    session.groups(),
                    name,
                    startTimestamp,
                    ctx.coreComponents().clock().nowMillis(),
                    Status.apply("OK"),
                    scala.Option.apply("200"),
                    scala.Option.apply("Message with id=" + matchEventId.getId() + 
                        " successfully sent and received")
                );
                
                // Continue with next action
                next.execute(session);
                
            } catch (Exception e) {
                // Log failure
                long startTimestamp = ctx.coreComponents().clock().nowMillis();
                String name = requestName.apply(session);
                
                statsEngine.logResponse(
                    session.scenario(),
                    session.groups(),
                    name,
                    startTimestamp,
                    ctx.coreComponents().clock().nowMillis(),
                    Status.apply("KO"),
                    scala.Option.apply("500"),
                    scala.Option.apply(e.getMessage())
                );
                
                // Continue with next action even on failure
                next.execute(session);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private T parseMessage(String messageBody) throws Exception {
        if (messageBody == null || typeRef.isEmpty()) {
            return null;
        }
        
        // Use Jackson ObjectMapper to parse JSON message
        com.fasterxml.jackson.databind.ObjectMapper mapper = 
            new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.readValue(messageBody, typeRef.get());
    }
}