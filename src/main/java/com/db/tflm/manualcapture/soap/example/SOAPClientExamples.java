package com.db.tflm.manualcapture.soap.example;

import com.db.tflm.manualcapture.soap.client.SecureSOAPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.ws.WebService;
import jakarta.xml.ws.WebMethod;
import java.net.URL;

/**
 * Examples demonstrating how to use certificate authentication with SOAP clients
 */
public class SOAPClientExamples {
    
    private static final Logger logger = LoggerFactory.getLogger(SOAPClientExamples.class);
    
    /**
     * Example service interface (you would replace this with your actual service interface)
     */
    @WebService
    public interface ExampleSoapService {
        @WebMethod
        String processRequest(String request);
        
        @WebMethod
        String getStatus();
    }
    
    /**
     * Example 1: Using keystore/truststore files
     */
    public static void exampleWithKeystoreFiles() {
        try {
            logger.info("Example 1: Using keystore/truststore files");
            
            // Create client with keystore configuration
            ExampleSoapService client = new SecureSOAPClient.Builder()
                .keystoreConfig("/path/to/client-keystore.p12", "keystorePassword", "PKCS12")
                .truststoreConfig("/path/to/truststore.jks", "truststorePassword")
                .buildCxfClient(ExampleSoapService.class, "https://example.com/soap/service");
            
            // Use the client
            String response = client.processRequest("Hello from client with keystore!");
            logger.info("Response: {}", response);
            
        } catch (Exception e) {
            logger.error("Error in keystore example", e);
        }
    }
    
    /**
     * Example 2: Using PEM certificate and key files
     */
    public static void exampleWithPEMFiles() {
        try {
            logger.info("Example 2: Using PEM certificate and key files");
            
            // Create client with PEM configuration
            ExampleSoapService client = new SecureSOAPClient.Builder()
                .pemConfig(
                    "/path/to/client-cert.pem",    // Client certificate
                    "/path/to/client-key.pem",     // Private key
                    "keyPassword",                  // Key password (optional)
                    "/path/to/ca-cert.pem"         // CA certificate (optional)
                )
                .buildCxfClient(ExampleSoapService.class, "https://example.com/soap/service");
            
            // Use the client
            String status = client.getStatus();
            logger.info("Status: {}", status);
            
        } catch (Exception e) {
            logger.error("Error in PEM example", e);
        }
    }
    
    /**
     * Example 3: Using JAX-WS with certificate authentication
     */
    public static void exampleWithJaxWS() {
        try {
            logger.info("Example 3: Using JAX-WS with certificate authentication");
            
            URL wsdlLocation = new URL("https://example.com/soap/service?wsdl");
            
            // Create JAX-WS client
            ExampleSoapService client = new SecureSOAPClient.Builder()
                .keystoreConfig("/path/to/client-keystore.p12", "password", "PKCS12")
                .truststoreConfig("/path/to/truststore.jks", "password")
                .buildJaxWsClient(ExampleSoapService.class, wsdlLocation, "https://example.com/soap/service");
            
            // Use the client
            String response = client.processRequest("JAX-WS request with certificates");
            logger.info("JAX-WS Response: {}", response);
            
        } catch (Exception e) {
            logger.error("Error in JAX-WS example", e);
        }
    }
    
    /**
     * Example 4: Development/testing configuration (accepts all certificates)
     * WARNING: Only use for development/testing!
     */
    public static void exampleForDevelopment() {
        try {
            logger.info("Example 4: Development configuration (accepts all certificates)");
            
            // Create client that accepts all certificates (for testing only!)
            ExampleSoapService client = new SecureSOAPClient.Builder()
                .acceptAllCertificates(true)
                .acceptAllHostnames(true)
                .buildCxfClient(ExampleSoapService.class, "https://dev-server.example.com/soap/service");
            
            // Use the client
            String response = client.processRequest("Development request");
            logger.info("Development Response: {}", response);
            
        } catch (Exception e) {
            logger.error("Error in development example", e);
        }
    }
    
    /**
     * Example 5: Manual configuration approach
     */
    public static void exampleManualConfiguration() {
        try {
            logger.info("Example 5: Manual configuration approach");
            
            // Create certificate config manually
            SecureSOAPClient.CertificateConfig config = new SecureSOAPClient.CertificateConfig()
                .setKeystorePath("/path/to/client-cert.p12")
                .setKeystorePassword("password")
                .setKeystoreType("PKCS12")
                .setTruststorePath("/path/to/truststore.jks")
                .setTruststorePassword("trustpass");
            
            // Create client using the config
            ExampleSoapService client = SecureSOAPClient.createCxfClient(
                ExampleSoapService.class,
                "https://example.com/soap/service",
                config
            );
            
            // Use the client
            String response = client.processRequest("Manual configuration request");
            logger.info("Manual Config Response: {}", response);
            
        } catch (Exception e) {
            logger.error("Error in manual configuration example", e);
        }
    }
    
    /**
     * Example 6: Global SSL configuration
     * This affects all HTTPS connections in the JVM
     */
    public static void exampleGlobalSSLConfiguration() {
        try {
            logger.info("Example 6: Global SSL configuration");
            
            // Configure global SSL settings
            SecureSOAPClient.CertificateConfig config = new SecureSOAPClient.CertificateConfig()
                .setKeystorePath("/path/to/global-client-cert.p12")
                .setKeystorePassword("password")
                .setTruststorePath("/path/to/global-truststore.jks")
                .setTruststorePassword("trustpass");
            
            SecureSOAPClient.configureGlobalSSL(config);
            
            // Now any SOAP client (or HTTPS connection) will use these certificates
            // You can create clients normally without additional SSL configuration
            ExampleSoapService client = new SecureSOAPClient.Builder()
                .buildCxfClient(ExampleSoapService.class, "https://example.com/soap/service");
            
            String response = client.processRequest("Global SSL request");
            logger.info("Global SSL Response: {}", response);
            
        } catch (Exception e) {
            logger.error("Error in global SSL example", e);
        }
    }
    
    /**
     * Example 7: Integration with existing Gatling test framework
     */
    public static void exampleGatlingIntegration() {
        try {
            logger.info("Example 7: Integration with Gatling test framework");
            
            // This shows how you might integrate SOAP calls into your existing Gatling tests
            // You could call SOAP services from within your Gatling scenarios
            
            ExampleSoapService soapClient = new SecureSOAPClient.Builder()
                .keystoreConfig("/path/to/test-client-cert.p12", "password", "PKCS12")
                .buildCxfClient(ExampleSoapService.class, "https://test-env.example.com/soap/service");
            
            // This could be called from within a Gatling scenario
            String soapResponse = soapClient.processRequest("Gatling test data");
            
            // You could then use this response in your Gatling test assertions
            logger.info("SOAP response for Gatling test: {}", soapResponse);
            
        } catch (Exception e) {
            logger.error("Error in Gatling integration example", e);
        }
    }
    
    /**
     * Main method to run all examples
     */
    public static void main(String[] args) {
        logger.info("Running SOAP client certificate authentication examples");
        
        // Note: These examples will fail unless you have actual certificates and services
        // They are provided to show the usage patterns
        
        // exampleWithKeystoreFiles();
        // exampleWithPEMFiles();
        // exampleWithJaxWS();
        // exampleForDevelopment();
        // exampleManualConfiguration();
        // exampleGlobalSSLConfiguration();
        // exampleGatlingIntegration();
        
        logger.info("Examples completed. Uncomment the method calls above to run specific examples.");
    }
}