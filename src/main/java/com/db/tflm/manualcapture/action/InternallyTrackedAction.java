package com.db.tflm.manualcapture.action;

import io.gatling.commons.stats.Status;
import io.gatling.commons.util.Clock;
import io.gatling.commons.validation.Failure;
import io.gatling.commons.validation.Success;
import io.gatling.commons.validation.Validation;
import io.gatling.core.CoreComponents;
import io.gatling.core.action.Action;
import io.gatling.core.action.RequestAction;
import io.gatling.core.session.Expression;
import io.gatling.core.session.Session;
import io.gatling.core.stats.StatsEngine;
import io.gatling.core.structure.ScenarioContext;
import java.util.function.BinaryOperator;

public class InternallyTrackedAction extends RequestAction {
    
    private final RequestAction requestWith;
    private final Tracker trackWith;
    private final Expression<String> requestName;
    private final ScenarioContext context;
    private final Action next;
    private final CoreComponents coreComponents;
    private final StatsEngine statsEngine;
    private final Clock clock;
    private final String name;

    public InternallyTrackedAction(RequestAction requestWith, 
                                 Tracker trackWith, 
                                 Expression<String> requestName,
                                 ScenarioContext context, 
                                 Action next) {
        this.requestWith = requestWith;
        this.trackWith = trackWith;
        this.requestName = requestName;
        this.context = context;
        this.next = next;
        this.coreComponents = context.coreComponents();
        this.statsEngine = coreComponents.statsEngine();
        this.clock = coreComponents.clock();
        this.name = "InternallyTrackedAction";
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public StatsEngine statsEngine() {
        return statsEngine;
    }

    @Override
    public Clock clock() {
        return clock;
    }

    @Override
    public Action next() {
        return next;
    }

    @Override
    public Validation<Void> sendRequest(Session session) {
        long startTime = clock.nowMillis();
        String reqName = requestName.apply(session).toOption().get();

        Validation<Void> requestValidation = requestWith.sendRequest(session);
        Validation<Void> trackerValidation = trackWith.track(session);

        Validation<Void> validation = mergeValidations(
            requestValidation, 
            trackerValidation, 
            (a, b) -> a + " - " + b
        );

        validation.onSuccess(result -> {
            statsEngine.logResponse(
                session.scenario(), 
                session.groups(), 
                reqName, 
                startTime, 
                clock.nowMillis(), 
                Status.apply("OK"), 
                scala.Option.empty(), 
                scala.Option.empty()
            );
            return null;
        });

        return validation;
    }

    private <A, B, C> Validation<Void> mergeValidations(Validation<A> validation1, 
                                                        Validation<B> validation2, 
                                                        BinaryOperator<String> mergeFn) {
        if (validation1.isSuccess() && validation2.isSuccess()) {
            A a = ((Success<A>) validation1).value();
            B b = ((Success<B>) validation2).value();
            return new Success<>(null);
        } else if (validation1.isFailure() && validation2.isFailure()) {
            String err1 = ((Failure) validation1).message();
            String err2 = ((Failure) validation2).message();
            return new Failure(err1 + err2);
        } else if (validation1.isFailure()) {
            String err = ((Failure) validation1).message();
            return new Failure(err);
        } else {
            String err = ((Failure) validation2).message();
            return new Failure(err);
        }
    }
}