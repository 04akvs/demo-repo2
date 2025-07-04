package com.db.tflm.manualcapture.gatling.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import com.db.tflm.manualcapture.gatling.util.TransactionUtil;
import java.time.Duration;

public class TransactionApprovalSimulation extends Simulation {

    // HTTP configuration
    private HttpProtocolBuilder httpProtocol = http
        .baseUrl("https://api.example.com")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling Performance Test");

    // Scenario definition
    private ScenarioBuilder approvalScenario = scenario("Transaction Approval Test")
        .exec(session -> {
            System.out.println("Starting transaction approval test for user: " + session.getString("userId"));
            return session;
        })
        .exec(TransactionUtil.requestTransactionApprovalTobbLimits("Transaction Approval Request"))
        .exec(session -> {
            String responseBody = session.getString("RESPONSE_BODY");
            System.out.println("Received response: " + responseBody);
            return session;
        });

    // Alternative scenario using expression-based request name
    private ScenarioBuilder dynamicApprovalScenario = scenario("Dynamic Transaction Approval Test")
        .exec(TransactionUtil.requestTransactionApprovalTobbLimits(
            session -> "Dynamic Request for " + session.getString("userId")
        ));

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