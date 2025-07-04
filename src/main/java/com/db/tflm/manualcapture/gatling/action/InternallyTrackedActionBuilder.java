package com.db.tflm.manualcapture.gatling.action;

import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import static io.gatling.javaapi.core.CoreDsl.*;

public class InternallyTrackedActionBuilder {
    
    private String requestName;
    
    private InternallyTrackedActionBuilder(String requestName) {
        this.requestName = requestName;
    }
    
    public static InternallyTrackedActionBuilder internallyTrackedAction(String requestName) {
        return new InternallyTrackedActionBuilder(requestName);
    }
    
    /**
     * Method to handle HTTP requests with internal tracking
     * Changed parameter from HttpRequestBuilder to HttpRequestActionBuilder
     * since HttpRequestActionBuilder is the final built form and doesn't have .build() method
     */
    public ChainBuilder requestWithHttp(HttpRequestActionBuilder httpRequestActionBuilder) {
        return exec(session -> {
            System.out.println("Executing internally tracked action: " + requestName);
            return session;
        })
        .exec(httpRequestActionBuilder)  // Use the already built HttpRequestActionBuilder directly
        .exec(session -> {
            System.out.println("Completed internally tracked action: " + requestName);
            return session;
        });
    }
}