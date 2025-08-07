package com.db.tflm.sdpingestion.util;

import com.db.sdp.common.NonVersionedIdentifier;
import com.db.tf.messaging.config.Topic;
import com.db.tf.messaging.producer.Producer;
import com.db.tflm.sdpingestion.test.MockInternalMessageReceiver;
import com.fasterxml.jackson.core.type.TypeReference;
import io.gatling.core.action.Action;
import io.gatling.core.structure.ScenarioContext;
import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.Session;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Simple ActionBuilder for KafkaJmsAction that integrates with Gatling stats
 */
public class KafkaJmsActionBuilder extends ActionBuilder {
    
    private final String requestName;
    private final Producer kafkaProducer;
    private final Topic topic;
    private final Function<Session, String> messageKeyFunc;
    private final MockInternalMessageReceiver jmsReceiver;
    private final TypeReference<?> typeRef;
    private final Function<Object, NonVersionedIdentifier> messageMatcherFunction;
    private final Consumer<Object> beforeSendCallback;

    public KafkaJmsActionBuilder(
            String requestName,
            Producer kafkaProducer,
            Topic topic,
            Function<Session, String> messageKeyFunc,
            MockInternalMessageReceiver jmsReceiver,
            TypeReference<?> typeRef,
            Function<Object, NonVersionedIdentifier> messageMatcherFunction,
            Consumer<Object> beforeSendCallback) {
        
        this.requestName = requestName;
        this.kafkaProducer = kafkaProducer;
        this.topic = topic;
        this.messageKeyFunc = messageKeyFunc;
        this.jmsReceiver = jmsReceiver;
        this.typeRef = typeRef;
        this.messageMatcherFunction = messageMatcherFunction;
        this.beforeSendCallback = beforeSendCallback;
    }

    @Override
    public Action build(ScenarioContext ctx, Action next) {
        return new KafkaJmsAction(
            requestName,
            kafkaProducer,
            topic,
            messageKeyFunc,
            jmsReceiver,
            typeRef,
            messageMatcherFunction,
            beforeSendCallback,
            next,
            ctx.coreComponents().statsEngine()
        );
    }
    
    /**
     * Factory method to create the action builder
     */
    public static KafkaJmsActionBuilder kafkaJmsAction(
            String requestName,
            Producer kafkaProducer,
            Topic topic,
            Function<Session, String> messageKeyFunc,
            MockInternalMessageReceiver jmsReceiver,
            TypeReference<?> typeRef,
            Function<Object, NonVersionedIdentifier> messageMatcherFunction,
            Consumer<Object> beforeSendCallback) {
        
        return new KafkaJmsActionBuilder(
            requestName, kafkaProducer, topic, messageKeyFunc, 
            jmsReceiver, typeRef, messageMatcherFunction, beforeSendCallback
        );
    }
}