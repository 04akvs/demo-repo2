import io.gatling.core.structure.ChainBuilder;
import io.gatling.core.session.Expression;
import io.gatling.http.action.HttpRequestActionBuilder;
import static io.gatling.core.CoreDsl.*;
import static io.gatling.http.HttpDsl.*;

public class JavaUsageExample {

    public static ChainBuilder requestTransactionApprovalToDbLimits(Expression<String> requestName) {
        return feed(createUserDecisionFeederForAction("APPROVE_LIMIT_REQUEST", 0))
            .exec(
                InternallyTrackedActionBuilder.internallyTrackedAction(requestName)
                    .requestWithHttp(
                        http("Send Approval Request")
                            .post("/api/manualCapture/#{sourceReference}:0/status")
                            .body(ElFileBody("performance/user_decision.json"))
                    )
                    .trackOnCreatedEvent(session -> session.getString("sourceReference"))
            )
            // DbLimits replies with an approval, should wait for the event to be consumed
            .exec(session -> {
                // DbLimitsMockResponseEvents.sendApprovalEvent(session, transactionId);
                return session;
            })
            .pause(10, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    // Example of how to create the feeder method (you'll need to implement this)
    private static io.gatling.core.feeder.Feeder<Object> createUserDecisionFeederForAction(String action, int entityVersion) {
        // Implementation depends on your specific feeder logic
        return null; // Replace with actual implementation
    }
}