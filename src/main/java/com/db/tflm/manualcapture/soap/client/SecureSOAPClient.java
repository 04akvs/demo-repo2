package com.db.tflm.manualcapture.soap.client;

import com.db.tflm.manualcapture.soap.util.CertificateManager;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.ws.*;
import jakarta.xml.ws.handler.MessageContext;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.namespace.QName;
import java.net.URL;
import java.util.Map;

/**
 * Secure SOAP client that supports certificate-based authentication
 * 
 * This client provides multiple ways to configure certificate authentication:
 * 1. Using keystore/truststore files
 * 2. Using PEM certificate and key files
 * 3. Using Apache CXF for advanced configuration
 */
public class SecureSOAPClient {
    
    private static final Logger logger = LoggerFactory.getLogger(SecureSOAPClient.class);
    
    /**
     * Configuration class for certificate authentication
     */
    public static class CertificateConfig {
        private String keystorePath;
        private String keystorePassword;
        private String keystoreType = "PKCS12";
        private String truststorePath;
        private String truststorePassword;
        private String pemCertPath;
        private String pemKeyPath;
        private String pemKeyPassword;
        private String pemCaCertPath;
        private boolean acceptAllCertificates = false;
        private boolean acceptAllHostnames = false;
        
        // Getters and setters
        public String getKeystorePath() { return keystorePath; }
        public CertificateConfig setKeystorePath(String keystorePath) { this.keystorePath = keystorePath; return this; }
        
        public String getKeystorePassword() { return keystorePassword; }
        public CertificateConfig setKeystorePassword(String keystorePassword) { this.keystorePassword = keystorePassword; return this; }
        
        public String getKeystoreType() { return keystoreType; }
        public CertificateConfig setKeystoreType(String keystoreType) { this.keystoreType = keystoreType; return this; }
        
        public String getTruststorePath() { return truststorePath; }
        public CertificateConfig setTruststorePath(String truststorePath) { this.truststorePath = truststorePath; return this; }
        
        public String getTruststorePassword() { return truststorePassword; }
        public CertificateConfig setTruststorePassword(String truststorePassword) { this.truststorePassword = truststorePassword; return this; }
        
        public String getPemCertPath() { return pemCertPath; }
        public CertificateConfig setPemCertPath(String pemCertPath) { this.pemCertPath = pemCertPath; return this; }
        
        public String getPemKeyPath() { return pemKeyPath; }
        public CertificateConfig setPemKeyPath(String pemKeyPath) { this.pemKeyPath = pemKeyPath; return this; }
        
        public String getPemKeyPassword() { return pemKeyPassword; }
        public CertificateConfig setPemKeyPassword(String pemKeyPassword) { this.pemKeyPassword = pemKeyPassword; return this; }
        
        public String getPemCaCertPath() { return pemCaCertPath; }
        public CertificateConfig setPemCaCertPath(String pemCaCertPath) { this.pemCaCertPath = pemCaCertPath; return this; }
        
        public boolean isAcceptAllCertificates() { return acceptAllCertificates; }
        public CertificateConfig setAcceptAllCertificates(boolean acceptAllCertificates) { this.acceptAllCertificates = acceptAllCertificates; return this; }
        
        public boolean isAcceptAllHostnames() { return acceptAllHostnames; }
        public CertificateConfig setAcceptAllHostnames(boolean acceptAllHostnames) { this.acceptAllHostnames = acceptAllHostnames; return this; }
    }
    
    /**
     * Creates a SOAP client using JAX-WS with certificate authentication
     * 
     * @param <T> Service interface type
     * @param serviceInterface The service interface class
     * @param wsdlLocation URL of the WSDL
     * @param endpointAddress Service endpoint address
     * @param certConfig Certificate configuration
     * @return Configured service client
     */
    public static <T> T createJaxWsClient(
            Class<T> serviceInterface,
            URL wsdlLocation,
            String endpointAddress,
            CertificateConfig certConfig) throws Exception {
        
        logger.info("Creating JAX-WS client for service: {}", serviceInterface.getName());
        
        // Create SSL context based on configuration
        SSLContext sslContext = createSSLContext(certConfig);
        
        // Create service
        Service service = Service.create(wsdlLocation, new QName("http://example.com/", serviceInterface.getSimpleName() + "Service"));
        T port = service.getPort(serviceInterface);
        
        // Configure SSL on the port
        configureSSLForJaxWsPort(port, sslContext, certConfig);
        
        // Set endpoint address if provided
        if (endpointAddress != null) {
            BindingProvider bindingProvider = (BindingProvider) port;
            bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
        }
        
        logger.info("JAX-WS client created successfully");
        return port;
    }
    
    /**
     * Creates a SOAP client using Apache CXF with certificate authentication
     * 
     * @param <T> Service interface type
     * @param serviceInterface The service interface class
     * @param endpointAddress Service endpoint address
     * @param certConfig Certificate configuration
     * @return Configured service client
     */
    public static <T> T createCxfClient(
            Class<T> serviceInterface,
            String endpointAddress,
            CertificateConfig certConfig) throws Exception {
        
        logger.info("Creating CXF client for service: {}", serviceInterface.getName());
        
        // Create CXF proxy factory
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(serviceInterface);
        factory.setAddress(endpointAddress);
        
        // Create the client
        @SuppressWarnings("unchecked")
        T client = (T) factory.create();
        
        // Configure SSL
        configureCxfClientSSL(client, certConfig);
        
        logger.info("CXF client created successfully");
        return client;
    }
    
    /**
     * Creates SSL context based on certificate configuration
     */
    private static SSLContext createSSLContext(CertificateConfig config) throws Exception {
        if (config.isAcceptAllCertificates()) {
            return CertificateManager.createAcceptAllSSLContext();
        } else if (config.getPemCertPath() != null && config.getPemKeyPath() != null) {
            return CertificateManager.createSSLContextFromPEM(
                config.getPemCertPath(),
                config.getPemKeyPath(),
                config.getPemKeyPassword(),
                config.getPemCaCertPath()
            );
        } else if (config.getKeystorePath() != null) {
            return CertificateManager.createSSLContextWithClientCert(
                config.getKeystorePath(),
                config.getKeystorePassword(),
                config.getKeystoreType(),
                config.getTruststorePath(),
                config.getTruststorePassword()
            );
        } else {
            throw new IllegalArgumentException("No valid certificate configuration provided");
        }
    }
    
    /**
     * Configures SSL for JAX-WS port
     */
    private static void configureSSLForJaxWsPort(Object port, SSLContext sslContext, CertificateConfig config) {
        BindingProvider bindingProvider = (BindingProvider) port;
        Map<String, Object> requestContext = bindingProvider.getRequestContext();
        
        // Set SSL socket factory
        requestContext.put("com.sun.xml.ws.transport.https.client.SSLSocketFactory", sslContext.getSocketFactory());
        
        // Set hostname verifier if needed
        if (config.isAcceptAllHostnames()) {
            requestContext.put("com.sun.xml.ws.transport.https.client.hostname.verifier", 
                              CertificateManager.createAcceptAllHostnameVerifier());
        }
        
        // Alternative approach using system properties for global SSL configuration
        System.setProperty("javax.net.ssl.keyStore", config.getKeystorePath() != null ? config.getKeystorePath() : "");
        System.setProperty("javax.net.ssl.keyStorePassword", config.getKeystorePassword() != null ? config.getKeystorePassword() : "");
        System.setProperty("javax.net.ssl.trustStore", config.getTruststorePath() != null ? config.getTruststorePath() : "");
        System.setProperty("javax.net.ssl.trustStorePassword", config.getTruststorePassword() != null ? config.getTruststorePassword() : "");
    }
    
    /**
     * Configures SSL for CXF client
     */
    private static void configureCxfClientSSL(Object client, CertificateConfig config) throws Exception {
        Client cxfClient = ClientProxy.getClient(client);
        HTTPConduit httpConduit = (HTTPConduit) cxfClient.getConduit();
        
        // Create TLS parameters
        TLSClientParameters tlsParams = new TLSClientParameters();
        
        // Set SSL context
        SSLContext sslContext = createSSLContext(config);
        tlsParams.setSSLSocketFactory(sslContext.getSocketFactory());
        
        // Configure hostname verification
        if (config.isAcceptAllHostnames()) {
            tlsParams.setDisableCNCheck(true);
            tlsParams.setHostnameVerifier(CertificateManager.createAcceptAllHostnameVerifier());
        }
        
        // Set TLS parameters on the HTTP conduit
        httpConduit.setTlsClientParameters(tlsParams);
        
        logger.info("SSL configuration applied to CXF client");
    }
    
    /**
     * Utility method to configure global SSL settings for the JVM
     * This affects all HTTPS connections in the application
     */
    public static void configureGlobalSSL(CertificateConfig config) throws Exception {
        logger.info("Configuring global SSL settings");
        
        SSLContext sslContext = createSSLContext(config);
        
        // Set as default SSL context
        SSLContext.setDefault(sslContext);
        
        // Set default socket factory for HTTPS connections
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        
        // Set hostname verifier if needed
        if (config.isAcceptAllHostnames()) {
            HttpsURLConnection.setDefaultHostnameVerifier(CertificateManager.createAcceptAllHostnameVerifier());
        }
        
        logger.info("Global SSL settings configured");
    }
    
    /**
     * Builder class for easier configuration
     */
    public static class Builder {
        private final CertificateConfig config = new CertificateConfig();
        
        public Builder keystoreConfig(String path, String password, String type) {
            config.setKeystorePath(path).setKeystorePassword(password).setKeystoreType(type);
            return this;
        }
        
        public Builder truststoreConfig(String path, String password) {
            config.setTruststorePath(path).setTruststorePassword(password);
            return this;
        }
        
        public Builder pemConfig(String certPath, String keyPath, String keyPassword, String caCertPath) {
            config.setPemCertPath(certPath).setPemKeyPath(keyPath)
                  .setPemKeyPassword(keyPassword).setPemCaCertPath(caCertPath);
            return this;
        }
        
        public Builder acceptAllCertificates(boolean accept) {
            config.setAcceptAllCertificates(accept);
            return this;
        }
        
        public Builder acceptAllHostnames(boolean accept) {
            config.setAcceptAllHostnames(accept);
            return this;
        }
        
        public <T> T buildJaxWsClient(Class<T> serviceInterface, URL wsdlLocation, String endpointAddress) throws Exception {
            return createJaxWsClient(serviceInterface, wsdlLocation, endpointAddress, config);
        }
        
        public <T> T buildCxfClient(Class<T> serviceInterface, String endpointAddress) throws Exception {
            return createCxfClient(serviceInterface, endpointAddress, config);
        }
        
        public CertificateConfig getConfig() {
            return config;
        }
    }
}