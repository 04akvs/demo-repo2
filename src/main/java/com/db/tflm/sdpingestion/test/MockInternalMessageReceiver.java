package com.db.tflm.sdpingestion.test;

import com.db.sdp.common.NonVersionedIdentifier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Mock implementation of internal message receiver for testing purposes
 */
public class MockInternalMessageReceiver {
    
    private long defaultTimeoutMs = 5000; // 5 seconds default timeout
    
    /**
     * Sets the default timeout for message receiving
     * 
     * @param timeoutMs Timeout in milliseconds
     */
    public void setDefaultTimeout(long timeoutMs) {
        this.defaultTimeoutMs = timeoutMs;
    }
    
    /**
     * Simulates receiving a message with the given identifier
     * 
     * @param identifier The message identifier to wait for
     * @return A future that completes when the message is "received"
     */
    public CompletableFuture<Object> get(NonVersionedIdentifier identifier) {
        return get(identifier, defaultTimeoutMs);
    }
    
    /**
     * Simulates receiving a message with the given identifier and timeout
     * 
     * @param identifier The message identifier to wait for
     * @param timeoutMs Timeout in milliseconds
     * @return A future that completes when the message is "received"
     */
    public CompletableFuture<Object> get(NonVersionedIdentifier identifier, long timeoutMs) {
        System.out.println("MockInternalMessageReceiver: Waiting for message with ID: " + identifier.getId());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate processing time
                Thread.sleep(Math.min(timeoutMs / 10, 100)); // 10% of timeout or 100ms max
                
                System.out.println("MockInternalMessageReceiver: Received message with ID: " + identifier.getId());
                
                // Return a mock message object
                return createMockMessage(identifier);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Message reception interrupted", e);
            }
        }).orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Creates a mock message object for testing
     * 
     * @param identifier The message identifier
     * @return A mock message object
     */
    private Object createMockMessage(NonVersionedIdentifier identifier) {
        return new Object() {
            @Override
            public String toString() {
                return "MockMessage{id=" + identifier.getId() + ", timestamp=" + System.currentTimeMillis() + "}";
            }
        };
    }
    
    /**
     * Simulates checking if a message is available
     * 
     * @param identifier The message identifier to check
     * @return true if a message is available (always true in mock)
     */
    public boolean isMessageAvailable(NonVersionedIdentifier identifier) {
        // In a real implementation, this would check the actual message queue
        return true;
    }
    
    /**
     * Clears any pending messages (for testing cleanup)
     */
    public void clear() {
        System.out.println("MockInternalMessageReceiver: Cleared all pending messages");
    }
}