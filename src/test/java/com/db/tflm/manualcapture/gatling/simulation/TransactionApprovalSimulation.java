package com.db.tflm.manualcapture.gatling.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import java.time.Duration;

public class TransactionApprovalSimulation extends Simulation {

    // HTTP configuration
    private HttpProtocolBuilder httpProtocol = http
        .baseUrl("https://api.example.com")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling Performance Test");

    // Feeder for user decision data
    private FeederBuilder<String> userDecisionFeeder = csv("performance/user_decision_data.csv").random();

    // Simplified scenario using standard Gatling HTTP DSL
    private ScenarioBuilder approvalScenario = scenario("Transaction Approval Test")
        .exec(session -> {
            System.out.println("Starting transaction approval test for user: " + session.getString("userId"));
            return session;
        })
        .feed(userDecisionFeeder)
        .exec(
            http("Transaction Approval Request")
                .post("/api/manualCapture/#{sourceReference}:0/status")
                .body(ElFileBody("performance/user_decision.json"))
                .check(bodyString().saveAs("RESPONSE_BODY"))
        )
        .exec(session -> {
            try {
                String responseBody = session.getString("RESPONSE_BODY");
                System.out.println("Received response: " + responseBody);
                
                // Add any additional processing logic here
                // For example, parsing JSON response or validating data
                
            } catch (Exception e) {
                System.err.println("Error processing response: " + e.getMessage());
                throw new RuntimeException(e);
            }
            return session;
        })
        .pause(Duration.ofMillis(10));

    // Alternative scenario with dynamic request naming
    private ScenarioBuilder dynamicApprovalScenario = scenario("Dynamic Transaction Approval Test")
        .exec(session -> {
            System.out.println("Starting dynamic transaction approval test for user: " + session.getString("userId"));
            return session;
        })
        .feed(userDecisionFeeder)
        .exec(
            http(session -> "Dynamic Request for " + session.getString("userId"))
                .post("/api/manualCapture/#{sourceReference}:0/status")
                .body(ElFileBody("performance/user_decision.json"))
                .check(bodyString().saveAs("RESPONSE_BODY"))
        )
        .exec(session -> {
            try {
                String responseBody = session.getString("RESPONSE_BODY");
                System.out.println("Dynamic response: " + responseBody);
            } catch (Exception e) {
                System.err.println("Error processing dynamic response: " + e.getMessage());
                throw new RuntimeException(e);
            }
            return session;
        })
        .pause(Duration.ofMillis(10));

    // Load test setup
    {
        setUp(
            approvalScenario.injectOpen(
                rampUsers(10).during(Duration.ofSeconds(30)),
                constantUsersPerSec(5).during(Duration.ofMinutes(2))
            ),
            dynamicApprovalScenario.injectOpen(
                rampUsers(5).during(Duration.ofSeconds(15))
            )
        )
        .protocols(httpProtocol)
        .assertions(
            global().responseTime().max().lt(5000),
            global().responseTime().mean().lt(2000),
            global().successfulRequests().percent().gt(95.0)
        );
    }
}