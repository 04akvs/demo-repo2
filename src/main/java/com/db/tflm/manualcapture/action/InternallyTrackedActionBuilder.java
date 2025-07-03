package com.db.tflm.manualcapture.action;

import io.gatling.commons.validation.Success;
import io.gatling.core.action.Action;
import io.gatling.core.action.RequestAction;
import io.gatling.core.action.builder.ActionBuilder;
import io.gatling.core.session.Expression;
import io.gatling.core.session.Session;
import io.gatling.core.structure.ScenarioContext;
import io.gatling.http.request.builder.HttpRequestBuilder;

import java.util.Optional;
import java.util.function.BiFunction;

public final class InternallyTrackedActionBuilder extends ActionBuilder {
    
    private final Expression<String> requestName;
    private Optional<BiFunction<ScenarioContext, Action, RequestAction>> requestActionBuilder;
    private Optional<Tracker> tracker;

    public InternallyTrackedActionBuilder(Expression<String> requestName) {
        this.requestName = requestName;
        this.requestActionBuilder = Optional.empty();
        this.tracker = Optional.empty();
    }

    public InternallyTrackedActionBuilder requestWithHttp(HttpRequestBuilder httpRequestBuilder) {
        this.requestActionBuilder = Optional.of((ctx, next) -> 
            (RequestAction) httpRequestBuilder.build(ctx, next)
        );
        return this;
    }

    public InternallyTrackedActionBuilder trackWith(Tracker tracker) {
        this.tracker = Optional.of(tracker);
        return this;
    }

    public InternallyTrackedActionBuilder trackOnCreatedEvent(Expression<String> transactionId) {
        this.tracker = Optional.of(new CreatedEventTracker(transactionId));
        return this;
    }

    public InternallyTrackedActionBuilder trackOnStatusChangedEvent(Expression<String> transactionId) {
        this.tracker = Optional.of(new StatusChangedEventTracker(transactionId));
        return this;
    }

    @Override
    public Action build(ScenarioContext ctx, Action next) {
        RequestAction requestAction = requestActionBuilder
            .orElseThrow(() -> new IllegalStateException("Request action builder must be set"))
            .apply(ctx, next);
            
        Tracker trackingAction = tracker.orElse(session -> new Success<>(null));

        return new InternallyTrackedAction(
            requestAction,
            trackingAction,
            requestName,
            ctx,
            next
        );
    }

    public static InternallyTrackedActionBuilder internallyTrackedAction(Expression<String> requestName) {
        return new InternallyTrackedActionBuilder(requestName);
    }
}