# Simplified Gatling Kafka/JMS Usage Example

This example shows how the converted code works without custom actions, using only standard Gatling Java DSL.

## Basic Usage Pattern

```java
// Standard Gatling scenario - no custom actions needed!
ScenarioBuilder scenario = scenario("Loan Processing")
    .repeat(10).on(
        // 1. Load test data
        feed(csv("test-data.csv").random())
        
        // 2. Load message body from file 
        .exec(ElFileBody("performance/loan.json"))
        
        // 3. Send to Kafka and wait for JMS response
        .exec(KafkaJmsUtil.sendToKafkaAndReceiveFromJms(
            "Process Loan Request",           // Request name
            kafkaProducer,                    // Kafka producer bean
            loanTopic,                        // Topic to send to
            session -> generateMessageKey(),   // Message key function
            session -> session.getString("gatling.core.body.string"), // Body from ElFileBody
            jmsReceiver,                      // JMS receiver mock
            new TypeReference<Loan>() {},     // Type for JSON deserialization
            loan -> loan.getEventId(),        // Function to extract event ID
            loan -> processLoanBeforeSend(loan) // Optional preprocessing
        ))
        
        // 4. Validate results
        .exec(session -> {
            if ("OK".equals(session.getString("status"))) {
                System.out.println("✅ Loan processed successfully");
            } else {
                System.out.println("❌ Loan processing failed: " + session.getString("error"));
            }
            return session;
        })
    );
```

## What Happens Under the Hood

1. **ElFileBody("performance/loan.json")** - Gatling loads the file and stores it in session under `"gatling.core.body.string"`

2. **KafkaJmsUtil.sendToKafkaAndReceiveFromJms(...)** - Returns a `Function<Session, Session>` that:
   - Gets the message body from session
   - Parses JSON to Java object
   - Sends to Kafka
   - Waits for JMS response
   - Stores timing and status in session

3. **Standard exec()** - Gatling executes the session function normally

## Comparison: Before vs After

### ❌ Before (Complex Custom Actions)
```java
// Required custom ActionBuilder, Action classes
.exec(
    KafkaRequestReplyJmsActionBuilder.<Loan>kafkaRequestReplyJms("Process Loan")
        .useProducer(kafkaProducer)
        .withPayload(ElFileBody("loan.json"), new TypeReference<Loan>() {})
        .toTopic(loanTopic, session -> generateKey())
        .waitForMessageWithInternalMessageReceiver(jmsReceiver, loan -> loan.getEventId())
        .beforeSendingMessage(loan -> processLoan(loan))
)
```

### ✅ After (Standard Gatling DSL)
```java
// Uses only standard Gatling features
.exec(ElFileBody("loan.json"))
.exec(KafkaJmsUtil.sendToKafkaAndReceiveFromJms(
    "Process Loan", kafkaProducer, loanTopic,
    session -> generateKey(),
    session -> session.getString("gatling.core.body.string"),
    jmsReceiver, new TypeReference<Loan>() {},
    loan -> loan.getEventId(),
    loan -> processLoan(loan)
))
```

## Benefits

- **Simpler**: No custom Gatling actions to maintain
- **Standard**: Uses only built-in Gatling DSL
- **Testable**: Session functions can be unit tested easily
- **Flexible**: Easy to compose with other Gatling features
- **Maintainable**: Less code, clearer intent

## Complete Working Example

```java
public class SimplifiedLoanSimulation extends Simulation {
    
    // Spring beans (same as before)
    private static final Producer kafkaProducer = getBean(Producer.class);
    private static final Topic loanTopic = getBean("loanTopic", Topic.class);
    private static final MockInternalMessageReceiver jmsReceiver = getBean(MockInternalMessageReceiver.class);
    
    // Simple scenario
    private ScenarioBuilder loanScenario = scenario("Loan Processing")
        .exec(ElFileBody("performance/loan.json"))
        .exec(KafkaJmsUtil.sendToKafkaAndReceiveFromJms(
            "Process Loan Request",
            kafkaProducer,
            loanTopic,
            session -> "LOAN-" + System.currentTimeMillis(),
            session -> session.getString("gatling.core.body.string"),
            jmsReceiver,
            new TypeReference<Loan>() {},
            loan -> loan.getExposure().getEventId(),
            loan -> {
                // Any preprocessing
                System.out.println("Processing loan: " + loan.getId());
            }
        ));

    // Standard Gatling setup
    {
        setUp(loanScenario.injectOpen(rampUsers(10).during(Duration.ofSeconds(30))))
            .assertions(global().successfulRequests().percent().gt(95.0));
    }
}
```