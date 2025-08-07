# Gatling Stats Integration Fix

## The Problem

When using session functions instead of proper Gatling actions, the following issues occur:

1. **No Request Tracking**: Requests don't appear in Gatling reports or group breakdowns
2. **Zero Statistics**: Global stats show `OK=0, KO=0` even when operations complete successfully  
3. **Assertion Failures**: `details()` assertions fail because no requests are tracked
4. **Infinite Report Generation**: Gatling hangs during report generation because the action chain never properly terminates

## The Root Cause

```java
// ❌ WRONG: Session functions don't integrate with Gatling stats
.exec(session -> {
    // Business logic here
    System.out.println("Request completed"); // Only console output
    return session.set("result", "done");   // No Gatling tracking
})
```

**Problems with this approach:**
- No stats logging to Gatling's `StatsEngine`
- No request names in reports
- No response times tracked
- Action chain may not terminate properly

## The Solution

Use a **simple custom action** that properly integrates with Gatling's stats system:

```java
// ✅ CORRECT: Custom action with proper Gatling integration
.exec(KafkaJmsActionBuilder.kafkaJmsAction(
    "Send Loan Request",              // Request name (appears in reports)
    kafkaProducer,
    loanTopic,
    session -> generateMessageKey(),
    jmsReceiver,
    new TypeReference<Loan>() {},
    loan -> loan.getEventId(),
    loan -> preprocessLoan(loan)
))
```

## Key Components

### 1. KafkaJmsAction.java
```java
public class KafkaJmsAction implements Action, NameGen {
    
    @Override
    public void execute(Session session) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Business logic here...
            
            // ✅ CRITICAL: Log to Gatling stats
            statsEngine.logResponse(
                session.scenario(),
                session.groups(),
                requestName,           // Shows in reports
                startTime,
                endTime,
                Status.apply("OK"),    // OK/KO status
                scala.Option.apply("200"),
                scala.Option.apply("Success message")
            );
            
        } catch (Exception e) {
            // ✅ Log failures too
            statsEngine.logResponse(/* ... KO status ... */);
        } finally {
            // ✅ CRITICAL: Always continue to prevent hanging
            next.execute(session);
        }
    }
}
```

### 2. KafkaJmsActionBuilder.java
```java
public class KafkaJmsActionBuilder extends ActionBuilder {
    
    @Override
    public Action build(ScenarioContext ctx, Action next) {
        return new KafkaJmsAction(
            // ... parameters ...
            ctx.coreComponents().statsEngine()  // ✅ Get Gatling's stats engine
        );
    }
}
```

## Results After Fix

### ✅ Before vs After

**Before (session functions):**
```
Loan simulation completed in 20 seconds
Global: OK=0, KO=0
No requests in breakdown
Report generation hangs
```

**After (proper action):**
```
Loan simulation completed in 20 seconds  
Global: OK=500, KO=0
Request breakdown shows:
├── Loan BOOKING_INITIATED group
│   └── Send instrument request to Kafka and receive in JMS: OK=100
├── Loan BOOKING_SUCCESSFUL group  
│   └── Send instrument request to Kafka and receive in JMS: OK=100
└── ...
```

### ✅ Features Now Working

1. **Request Tracking**: All requests appear in reports
2. **Group Breakdown**: Requests grouped properly by scenario groups
3. **Assertions**: `details()` assertions work correctly
4. **Response Times**: Min/max/mean/percentile metrics tracked
5. **Clean Termination**: Reports generate quickly without hanging

## Usage Pattern

```java
// Complete working example
ScenarioBuilder loanScenario = scenario("Loan Processing")
    .group("BOOKING_INITIATED").on(
        repeat(10).on(
            feed(eventFeeder.get())
            .exec(ElFileBody("performance/loan.json"))
            .exec(kafkaJmsAction(
                "Process Loan Request",
                kafkaProducer,
                loanTopic,
                session -> generateMessageKey(),
                jmsReceiver, 
                new TypeReference<Loan>() {},
                loan -> loan.getExposure().getEventId(),
                loan -> preprocessLoan(loan)
            ))
        )
    );

// Assertions now work!
setUp(loanScenario.injectOpen(rampUsers(10).during(Duration.ofSeconds(30))))
    .assertions(
        global().successfulRequests().percent().gt(95.0),
        details("BOOKING_INITIATED" + " / " + "Process Loan Request")
            .responseTime().max().lt(5000)
    );
```

## Key Lessons

1. **Stats Integration Required**: For proper Gatling integration, you need a custom Action that calls `statsEngine.logResponse()`
2. **Action Chain Termination**: Always call `next.execute(session)` in `finally` block
3. **Request Names Matter**: Use descriptive names that will appear in reports
4. **Group Integration**: Actions automatically inherit the current group context
5. **Error Handling**: Log both success and failure cases to stats engine

The simple custom action approach gives you the best of both worlds: easy implementation with full Gatling integration!