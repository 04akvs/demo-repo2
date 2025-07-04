package com.db.tflm.manualcapture.gatling.action;

import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.HttpRequestBuilder;
import io.gatling.core.action.Action;
import io.gatling.core.structure.ScenarioContext;
import java.util.function.BiFunction;
import java.util.Optional;
import static io.gatling.javaapi.core.CoreDsl.*;

public class InternallyTrackedActionBuilder extends ActionBuilder {
    
    private final Session.Expression<String> requestName;
    private Optional<BiFunction<ScenarioContext, Action, Object>> requestActionBuilder = Optional.empty();
    private Optional<Object> tracker = Optional.empty();
    
    public InternallyTrackedActionBuilder(Session.Expression<String> requestName) {
        this.requestName = requestName;
    }
    
    public static InternallyTrackedActionBuilder internallyTrackedAction(Session.Expression<String> requestName) {
        return new InternallyTrackedActionBuilder(requestName);
    }
    
    public static InternallyTrackedActionBuilder internallyTrackedAction(String requestName) {
        return new InternallyTrackedActionBuilder(session -> requestName);
    }
    
    /**
     * Maintains the same logic as Scala version:
     * Takes HttpRequestBuilder and calls .build(ctx, next) on it
     */
    public InternallyTrackedActionBuilder requestWithHttp(HttpRequestBuilder httpRequestBuilder) {
        this.requestActionBuilder = Optional.of((ctx, next) -> {
            // This is the equivalent of httpRequestBuilder.build(ctx, next).asInstanceOf[RequestAction]
            return httpRequestBuilder.build(ctx, next);
        });
        return this;
    }
    
    @Override
    public Action build(ScenarioContext ctx, Action next) {
        if (requestActionBuilder.isPresent()) {
            BiFunction<ScenarioContext, Action, Object> builder = requestActionBuilder.get();
            return (Action) builder.apply(ctx, next);
        }
        return next;
    }
}