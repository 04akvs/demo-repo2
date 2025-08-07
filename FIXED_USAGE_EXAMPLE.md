# Fixed Usage Example for eventType Deserialization

## The Issue

The error `Cannot deserialize value of type tradesense.kanon.InstrumentEventType from String "${eventType}"` happens because:

1. The JSON template contains `"eventType": "${eventType}"` 
2. Gatling's EL (Expression Language) needs session data to resolve `${eventType}`
3. The feeder must put the eventType value in the session before ElFileBody processes it

## The Solution

### 1. JSON Template (e.g., `performance/loan.json`)

```json
{
  "eventType": "${eventType}",
  "instrumentId": "${instrumentId}",
  "timestamp": "${timestamp}",
  "position": {
    "payload": {
      "position": {
        "positionQuantity": 1000000,
        "currency": {
          "id": "USD"
        }
      }
    }
  },
  "exposure": {
    "eventId": {
      "id": "EVENT_${instrumentId}_${timestamp}"
    }
  },
  "loan": {
    "payload": {
      "loanId": "LOAN_${instrumentId}",
      "amount": 1000000
    }
  }
}
```

### 2. Corrected Flow

```java
// This now works correctly:
return repeat(repeatNum).on(
    feed(eventFeeder.get())          // Puts "eventType": "BOOKING_INITIATED" in session
    .exec(ElFileBody(filePath))      // Resolves ${eventType} -> "BOOKING_INITIATED" 
    .exec(KafkaJmsUtil.sendToKafkaAndReceiveFromJms(
        ScenarioConstants.SEND_INSTRUMENT_REQUEST_TO_KAFKA_RECEIVE_IN_JMS,
        producer,
        instrumentTopic,
        session -> getKafkaMessageKey(),
        internalMessageReceiver,
        new TypeReference<Instrument>() {},
        instrument -> instrument.getExposure().getEventId(),
        beforeSendingMessage
    ))
);
```

### 3. What Happens Step by Step

1. **`feed(eventFeeder.get())`** 
   - Puts `eventType: "BOOKING_INITIATED"` in session
   - Puts other variables like `instrumentId`, `timestamp` in session

2. **`ElFileBody(filePath)`**
   - Reads the JSON template file
   - Resolves `${eventType}` → `"BOOKING_INITIATED"`
   - Resolves `${instrumentId}` → `"INSTR_1234567890"`
   - Stores the resolved JSON in session under `"gatling.core.body.string"`

3. **`KafkaJmsUtil.sendToKafkaAndReceiveFromJms(...)`**
   - Gets the resolved JSON from session using `getMessageBodyFromSession()`
   - Parses it with Jackson: `"eventType": "BOOKING_INITIATED"` → `InstrumentEventType.BOOKING_INITIATED`
   - Sends to Kafka and waits for JMS response

### 4. Key Points

✅ **Feed first**: Always call `feed()` before `ElFileBody()`  
✅ **String values**: EventType in JSON should be string (`"BOOKING_INITIATED"`)  
✅ **Enum parsing**: Jackson automatically converts string to enum  
✅ **Session resolution**: ElFileBody resolves all `${variable}` expressions  

### 5. Example JSON Output After Resolution

```json
{
  "eventType": "BOOKING_INITIATED",
  "instrumentId": "INSTR_1701234567890",
  "timestamp": 1701234567890,
  "position": {
    "payload": {
      "position": {
        "positionQuantity": 1000000,
        "currency": {
          "id": "USD"
        }
      }
    }
  },
  "exposure": {
    "eventId": {
      "id": "EVENT_INSTR_1701234567890_1701234567890"
    }
  },
  "loan": {
    "payload": {
      "loanId": "LOAN_INSTR_1701234567890",
      "amount": 1000000
    }
  }
}
```

### 6. If You Still Get Null Message Body

If `getMessageBodyFromSession()` returns null, it means ElFileBody didn't store the content properly. Check:

1. File path is correct and file exists
2. Feed was called before ElFileBody
3. Session variables are properly set by the feeder
4. JSON template has correct syntax

### 7. Debug Session Content

Add this to debug what's in the session:

```java
.exec(session -> {
    System.out.println("Session keys: " + session.attributes().keySet());
    System.out.println("Body: " + session.getString("gatling.core.body.string"));
    System.out.println("EventType: " + session.getString("eventType"));
    return session;
})
```

This should resolve both the deserialization error and the null message body issue!