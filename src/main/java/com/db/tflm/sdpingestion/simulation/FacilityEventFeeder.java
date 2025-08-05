package com.db.tflm.sdpingestion.simulation;

import com.db.tradesense.tflm.FacilityEventType;
import io.gatling.javaapi.core.FeederBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static io.gatling.javaapi.core.CoreDsl.*;

public class FacilityEventFeeder implements Supplier<FeederBuilder<String>> {
    
    private final FacilityEventType eventType;
    
    public FacilityEventFeeder(FacilityEventType eventType) {
        this.eventType = eventType;
    }
    
    @Override
    public FeederBuilder<String> get() {
        Map<String, Object> data = new HashMap<>();
        data.put("eventType", eventType.name());
        data.put("facilityId", "FAC_" + System.currentTimeMillis());
        data.put("timestamp", System.currentTimeMillis());
        
        return listFeeder(data).random();
    }
}