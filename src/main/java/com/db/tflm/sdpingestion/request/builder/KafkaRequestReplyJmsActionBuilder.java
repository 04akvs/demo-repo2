package com.db.tflm.sdpingestion.request.builder;

import com.db.sdp.common.NonVersionedIdentifier;
import com.db.tf.messaging.config.Topic;
import com.db.tf.messaging.producer.Producer;
import com.db.tflm.sdpingestion.request.actions.KafkaRequestReplyJmsAction;
import com.db.tflm.sdpingestion.test.MockInternalMessageReceiver;
import com.fasterxml.jackson.core.type.TypeReference;
import io.gatling.core.action.Action;
import io.gatling.core.structure.ScenarioContext;
import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.Body;
import io.gatling.javaapi.core.Session.Expression;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class KafkaRequestReplyJmsActionBuilder<T> extends ActionBuilder {
    
    private Expression<String> requestName;
    private Optional<Topic> topic = Optional.empty();
    private Optional<Body.WithString> message = Optional.empty();
    private Optional<Expression<String>> messageKey = Optional.empty();
    private Optional<Producer> kafkaProducer = Optional.empty();
    private Optional<MockInternalMessageReceiver> internalMessageReceiver = Optional.empty();
    private Optional<Consumer<T>> beforeSendMessage = Optional.empty();
    private Optional<TypeReference<T>> typeRef = Optional.empty();
    private Optional<Function<T, NonVersionedIdentifier>> nonVersionedIdentifierMessageMatcher = Optional.empty();

    public KafkaRequestReplyJmsActionBuilder(Expression<String> requestName) {
        this.requestName = requestName;
    }

    /**
     * Static factory method to create a new builder
     */
    public static <T> KafkaRequestReplyJmsActionBuilder<T> kafkaRequestReplyJms(Expression<String> requestName) {
        return new KafkaRequestReplyJmsActionBuilder<>(requestName);
    }

    /**
     * Static factory method with string request name
     */
    public static <T> KafkaRequestReplyJmsActionBuilder<T> kafkaRequestReplyJms(String requestName) {
        return new KafkaRequestReplyJmsActionBuilder<>(session -> requestName);
    }

    /**
     * Sets the topic and message key for Kafka publishing
     */
    public KafkaRequestReplyJmsActionBuilder<T> toTopic(Topic topic, Expression<String> messageId) {
        this.topic = Optional.of(topic);
        this.messageKey = Optional.of(messageId);
        return this;
    }

    /**
     * Sets the topic and message key with string message ID
     */
    public KafkaRequestReplyJmsActionBuilder<T> toTopic(Topic topic, String messageId) {
        this.topic = Optional.of(topic);
        this.messageKey = Optional.of(session -> messageId);
        return this;
    }

    /**
     * Sets the Kafka producer to use
     */
    public KafkaRequestReplyJmsActionBuilder<T> useProducer(Producer producer) {
        this.kafkaProducer = Optional.of(producer);
        return this;
    }

    /**
     * Sets up waiting for messages with internal message receiver
     */
    public KafkaRequestReplyJmsActionBuilder<T> waitForMessageWithInternalMessageReceiver(
            MockInternalMessageReceiver internalMessageReceiver,
            Function<T, NonVersionedIdentifier> nonVersionedIdentifierMessageMatcher) {
        this.internalMessageReceiver = Optional.of(internalMessageReceiver);
        this.nonVersionedIdentifierMessageMatcher = Optional.of(nonVersionedIdentifierMessageMatcher);
        return this;
    }

    /**
     * Sets a callback to execute before sending the message
     */
    public KafkaRequestReplyJmsActionBuilder<T> beforeSendingMessage(Consumer<T> beforeSend) {
        this.beforeSendMessage = Optional.of(beforeSend);
        return this;
    }

    /**
     * Sets the message payload and type reference
     */
    public KafkaRequestReplyJmsActionBuilder<T> withPayload(Body.WithString message, TypeReference<T> typeReference) {
        this.typeRef = Optional.of(typeReference);
        this.message = Optional.of(message);
        return this;
    }

    @Override
    public Action build(ScenarioContext ctx, Action next) {
        return new KafkaRequestReplyJmsAction<>(
            ctx,
            kafkaProducer,
            internalMessageReceiver,
            topic,
            message,
            messageKey,
            beforeSendMessage,
            typeRef,
            nonVersionedIdentifierMessageMatcher,
            requestName,
            next
        );
    }
}