package com.db.tflm.manualcapture.action;

import io.gatling.commons.validation.Success;
import io.gatling.commons.validation.Validation;
import io.gatling.core.session.Expression;
import io.gatling.core.session.Session;

public class StatusChangedEventTracker implements Tracker {
    
    private final Expression<String> transactionId;

    public StatusChangedEventTracker(Expression<String> transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public Validation<Void> track(Session session) {
        // Implementation for tracking status changed events
        // This would typically involve logging or recording the status change
        // For now, returning success - implement specific tracking logic as needed
        try {
            String txnId = transactionId.apply(session).toOption().get();
            // Add your status changed event tracking logic here
            // e.g., log the status change, send to monitoring system, etc.
            
            return new Success<>(null);
        } catch (Exception e) {
            return Validation.failure(e.getMessage());
        }
    }
}