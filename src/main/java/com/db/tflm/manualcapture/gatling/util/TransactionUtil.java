package com.db.tflm.manualcapture.gatling.util;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.HttpRequestBuilder;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import com.db.tflm.manualcapture.gatling.action.InternallyTrackedActionBuilder;
import java.time.Duration;

public class TransactionUtil {
    
    // Enum for action types
    public enum ActionType {
        APPROVE_LIMIT_REQUEST
    }
    
    /**
     * Creates a user decision feeder for the specified action type
     */
    public static FeederBuilder<String> createUserDecisionFeederForAction(ActionType actionType, int entityVersion) {
        // Implementation for creating feeder based on action type and entity version
        return csv("performance/user_decision_data.csv").random();
    }
    
    /**
     * Main method for requesting transaction approval to bb limits
     * Fixed to pass HttpRequestBuilder (not HttpRequestActionBuilder) to maintain Scala logic
     */
    public static ChainBuilder requestTransactionApprovalTobbLimits(String requestName) {
        return feed(createUserDecisionFeederForAction(ActionType.APPROVE_LIMIT_REQUEST, 8))
            .exec(
                InternallyTrackedActionBuilder.internallyTrackedAction(requestName)
                .requestWithHttp(
                    // Create HttpRequestBuilder - this is the builder before .body() and .check()
                    http("Send Approval Request")
                    .post("/api/manualCapture/#{sourceReference}:0/status")
                    .body(ElFileBody("performance/user_decision.json"))
                    .check(bodyString().saveAs("RESPONSE_BODY"))
                    // The .build() will be called inside requestWithHttp method
                )
            )
            .exec(session -> {
                try {
                    // Process the response body
                    String responseBody = session.getString("RESPONSE_BODY");
                    System.out.println("Response Body: " + responseBody);
                    
                    // Add any additional processing logic here
                    // For example, parsing JSON response or validating data
                    
                } catch (Exception e) {
                    System.err.println("Error processing response: " + e.getMessage());
                    throw new RuntimeException(e);
                }
                return session;
            })
            .pause(Duration.ofMillis(10));
    }
    
    /**
     * Alternative method using expression for dynamic request names
     */
    public static ChainBuilder requestTransactionApprovalTobbLimits(Session.Expression<String> requestNameExpression) {
        return feed(createUserDecisionFeederForAction(ActionType.APPROVE_LIMIT_REQUEST, 8))
            .exec(
                InternallyTrackedActionBuilder.internallyTrackedAction(requestNameExpression)
                .requestWithHttp(
                    http("Send Approval Request")
                    .post("/api/manualCapture/#{sourceReference}:0/status")
                    .body(ElFileBody("performance/user_decision.json"))
                    .check(bodyString().saveAs("RESPONSE_BODY"))
                )
            )
            .exec(session -> {
                try {
                    String responseBody = session.getString("RESPONSE_BODY");
                    System.out.println("Response Body: " + responseBody);
                } catch (Exception e) {
                    System.err.println("Error processing response: " + e.getMessage());
                    throw new RuntimeException(e);
                }
                return session;
            })
            .pause(Duration.ofMillis(10));
    }
}