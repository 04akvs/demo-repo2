# Gatling Java API Architecture Explained

## Your Questions Are Spot On! 

You've identified critical aspects of Gatling's Java API that many developers miss:

### 1. **Java ActionBuilder is Just a Wrapper**

```java
// Gatling Java ActionBuilder (from io.gatling.javaapi.core)
public abstract class ActionBuilder {
    // NO build() method defined here!
    // This is just a wrapper around Scala ActionBuilder
}
```

**You're correct**: The Java `ActionBuilder` interface is designed to **wrap existing Scala-based actions**, not to define custom Java actions from scratch.

### 2. **Session Functions Are NOT Tracked as Requests**

```java
// ❌ This is NOT tracked as a request in Gatling reports
.exec(session -> {
    // This is just a function execution
    // No stats logged, no request tracking, no group association
    System.out.println("This doesn't appear in reports");
    return session.set("result", "done");
})
```

**You're absolutely right**: `exec(session -> {...})` is treated as a simple function, not as a Gatling request.

## The Real Gatling Architecture

### Java API is a Facade Over Scala Core

```
┌─────────────────────────────────────┐
│           Java API Layer           │  ← Facade/Wrapper
│  (io.gatling.javaapi.core.*)       │
└─────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│          Scala Core Engine         │  ← Real Implementation
│  (io.gatling.core.*)                │
└─────────────────────────────────────┘
```

### How Stats Integration Really Works

Only **real Gatling Actions** (implementing `io.gatling.core.action.Action`) get tracked:

```scala
// Scala - This is what ACTUALLY gets tracked
class MyAction extends Action {
  def execute(session: Session): Unit = {
    // Business logic
    statsEngine.logResponse(...)  // ← This logs to reports
    next ! session               // ← This continues the chain
  }
}
```

## Three Approaches for Custom Actions

### ❌ **Approach 1: Pure Session Functions** 
```java
// This DOES NOT appear in reports or group breakdowns
.exec(session -> {
    // Business logic here
    return session;
})
```
- ✅ Simple to write
- ❌ No stats tracking
- ❌ No request names in reports  
- ❌ No group breakdown
- ❌ No assertions work

### ✅ **Approach 2: Scala Action + Java Wrapper**
```java
// Create a Scala Action and wrap it for Java use
public class MyActionBuilder extends ActionBuilder {
    @Override
    public io.gatling.core.action.builder.ActionBuilder asScala() {
        return new MyScalaActionBuilder(...);
    }
}
```
- ✅ Full Gatling integration
- ✅ Proper stats tracking
- ❌ Requires Scala knowledge
- ❌ More complex

### ✅ **Approach 3: Direct Scala Action Implementation**
```java
// Implement Scala Action interface directly in Java
public class MyAction implements io.gatling.core.action.Action, NameGen {
    
    @Override
    public void execute(io.gatling.core.session.Session session) {
        // Use Scala Session, not Java Session
        statsEngine.logResponse(
            session.scenario(),
            session.groups(), 
            requestName,
            startTime,
            endTime,
            Status.apply("OK"),
            scala.Option.apply("200"),
            scala.Option.apply("Success")
        );
        next.execute(session);
    }
}
```
- ✅ Full Gatling integration  
- ✅ Can be written in Java
- ❌ Must use Scala types
- ❌ More complex than session functions

## The Correct Solution for Your Use Case

Based on your requirements (stats tracking, group breakdown, assertions), you need **Approach 3**:

```java
// ✅ CORRECT: Direct Scala Action implementation
public class KafkaJmsAction implements io.gatling.core.action.Action, NameGen {
    
    private final StatsEngine statsEngine;
    private final io.gatling.core.action.Action next;
    // ... other fields

    @Override
    public void execute(io.gatling.core.session.Session session) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Your Kafka/JMS business logic here
            
            long endTime = System.currentTimeMillis();
            
            // ✅ This WILL appear in reports and support assertions
            statsEngine.logResponse(
                session.scenario(),
                session.groups(),
                "My Kafka Request",  // ← Shows in group breakdown
                startTime,
                endTime,
                Status.apply("OK"),
                scala.Option.apply("200"),
                scala.Option.apply("Success message")
            );
            
        } catch (Exception e) {
            // Log failure
            statsEngine.logResponse(/* ... KO status ... */);
        } finally {
            // ✅ CRITICAL: Always continue the chain
            next.execute(session);
        }
    }
}
```

## ActionBuilder Implementation

```java
public class KafkaJmsActionBuilder extends ActionBuilder {
    
    @Override
    public io.gatling.core.action.builder.ActionBuilder asScala() {
        // Return a Scala ActionBuilder that creates your Java Action
        return new io.gatling.core.action.builder.ActionBuilder() {
            @Override
            public io.gatling.core.action.Action build(
                ScenarioContext ctx, 
                io.gatling.core.action.Action next) {
                
                return new KafkaJmsAction(
                    ctx.coreComponents().statsEngine(),
                    next,
                    // ... other parameters
                );
            }
        };
    }
}
```

## Key Insights

### 1. **Java API Limitations**
- Java ActionBuilder is intentionally limited
- It's a wrapper, not a full implementation framework
- You CANNOT create fully custom actions using only Java API

### 2. **Stats Integration Requirements**
- Only Scala Actions get tracked in reports
- Session functions are invisible to Gatling's stats engine
- You MUST implement `io.gatling.core.action.Action` for tracking

### 3. **The Trade-off**
```java
// Simple but invisible
.exec(session -> doWork())       // ❌ No tracking

// Complex but tracked  
.exec(new MyCustomAction())      // ✅ Full integration
```

## Final Recommendation

For your Kafka/JMS use case where you need:
- ✅ Request tracking in reports
- ✅ Group breakdown  
- ✅ Assertion support
- ✅ Response time metrics

You **MUST** use the Scala Action approach. Session functions will never provide the integration you need.

The code I provided earlier with `KafkaJmsAction` implementing `io.gatling.core.action.Action` is the correct approach!