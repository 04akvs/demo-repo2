package com.db.tflm.sdpingestion;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Singleton Spring Application Context for accessing Spring beans
 * in Gatling simulations.
 */
public class SpringApplicationContext {
    
    private static SpringApplicationContext instance;
    private ApplicationContext applicationContext;
    
    private SpringApplicationContext() {
        // Initialize Spring context
        // This would typically load your Spring configuration
        this.applicationContext = new AnnotationConfigApplicationContext();
        // Add configuration classes here if needed
        // ((AnnotationConfigApplicationContext) applicationContext).register(YourConfigClass.class);
        // ((AnnotationConfigApplicationContext) applicationContext).refresh();
    }
    
    /**
     * Get the singleton instance
     */
    public static SpringApplicationContext getInstance() {
        if (instance == null) {
            synchronized (SpringApplicationContext.class) {
                if (instance == null) {
                    instance = new SpringApplicationContext();
                }
            }
        }
        return instance;
    }
    
    /**
     * Get the Spring ApplicationContext
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
    
    /**
     * Close the Spring context
     */
    public void close() {
        if (applicationContext instanceof AnnotationConfigApplicationContext) {
            ((AnnotationConfigApplicationContext) applicationContext).close();
        }
        instance = null;
    }
}