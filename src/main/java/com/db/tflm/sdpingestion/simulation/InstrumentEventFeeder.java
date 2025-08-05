package com.db.tflm.sdpingestion.simulation;

import com.db.tradesense.kannon.InstrumentEventType;
import io.gatling.javaapi.core.FeederBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static io.gatling.javaapi.core.CoreDsl.*;

public class InstrumentEventFeeder implements Supplier<FeederBuilder<String>> {
    
    private final InstrumentEventType eventType;
    
    public InstrumentEventFeeder(InstrumentEventType eventType) {
        this.eventType = eventType;
    }
    
    @Override
    public FeederBuilder<String> get() {
        Map<String, Object> data = new HashMap<>();
        data.put("eventType", eventType.name());
        data.put("instrumentId", "INSTR_" + System.currentTimeMillis());
        data.put("timestamp", System.currentTimeMillis());
        
        // Create a single record feeder
        return listFeeder(java.util.Collections.singletonList(data)).circular();
    }
}