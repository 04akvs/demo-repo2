package com.db.tflm.manualcapture.soap.util;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for managing SSL certificates and creating secure connections for SOAP requests
 */
public class CertificateManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CertificateManager.class);
    
    /**
     * Creates an SSL context with client certificate authentication
     * 
     * @param keystorePath Path to the keystore containing client certificate
     * @param keystorePassword Password for the keystore
     * @param keystoreType Type of keystore (JKS, PKCS12, etc.)
     * @param truststorePath Path to the truststore containing trusted CA certificates
     * @param truststorePassword Password for the truststore
     * @return Configured SSLContext
     */
    public static SSLContext createSSLContextWithClientCert(
            String keystorePath, 
            String keystorePassword,
            String keystoreType,
            String truststorePath, 
            String truststorePassword) throws Exception {
        
        logger.info("Creating SSL context with client certificate authentication");
        
        // Load client certificate keystore
        KeyStore clientKeyStore = KeyStore.getInstance(keystoreType != null ? keystoreType : "PKCS12");
        try (FileInputStream keystoreStream = new FileInputStream(keystorePath)) {
            clientKeyStore.load(keystoreStream, keystorePassword.toCharArray());
        }
        
        // Initialize key manager with client certificate
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, keystorePassword.toCharArray());
        
        // Load trusted CA certificates if truststore is provided
        TrustManager[] trustManagers = null;
        if (truststorePath != null) {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (FileInputStream truststoreStream = new FileInputStream(truststorePath)) {
                trustStore.load(truststoreStream, truststorePassword != null ? truststorePassword.toCharArray() : null);
            }
            
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        } else {
            // Use system default trust managers
            trustManagers = new TrustManager[] { new AcceptAllTrustManager() };
            logger.warn("No truststore provided, using permissive trust manager. This should not be used in production!");
        }
        
        // Create and initialize SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, new SecureRandom());
        
        logger.info("SSL context created successfully");
        return sslContext;
    }
    
    /**
     * Creates an SSL context from PEM formatted certificate and private key files
     * 
     * @param certPath Path to the PEM certificate file
     * @param keyPath Path to the PEM private key file
     * @param keyPassword Password for the private key (optional)
     * @param caCertPath Path to the CA certificate file (optional)
     * @return Configured SSLContext
     */
    public static SSLContext createSSLContextFromPEM(
            String certPath, 
            String keyPath, 
            String keyPassword,
            String caCertPath) throws Exception {
        
        logger.info("Creating SSL context from PEM files");
        
        // Create temporary keystore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        
        // Load certificate
        X509Certificate certificate = loadCertificateFromPEM(certPath);
        
        // Load private key
        PrivateKey privateKey = loadPrivateKeyFromPEM(keyPath, keyPassword);
        
        // Add certificate and key to keystore
        keyStore.setKeyEntry("client", privateKey, "changeit".toCharArray(), new X509Certificate[]{certificate});
        
        // Initialize key manager
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "changeit".toCharArray());
        
        // Setup trust managers
        TrustManager[] trustManagers;
        if (caCertPath != null) {
            X509Certificate caCert = loadCertificateFromPEM(caCertPath);
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca", caCert);
            
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        } else {
            trustManagers = new TrustManager[] { new AcceptAllTrustManager() };
            logger.warn("No CA certificate provided, using permissive trust manager");
        }
        
        // Create SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, new SecureRandom());
        
        logger.info("SSL context from PEM files created successfully");
        return sslContext;
    }
    
    /**
     * Loads X.509 certificate from PEM file
     */
    private static X509Certificate loadCertificateFromPEM(String certPath) throws Exception {
        try (FileInputStream fis = new FileInputStream(certPath)) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(fis);
        }
    }
    
    /**
     * Loads private key from PEM file
     */
    private static PrivateKey loadPrivateKeyFromPEM(String keyPath, String password) throws Exception {
        try (FileReader keyReader = new FileReader(keyPath)) {
            StringBuilder keyContent = new StringBuilder();
            char[] buffer = new char[1024];
            int length;
            while ((length = keyReader.read(buffer)) != -1) {
                keyContent.append(buffer, 0, length);
            }
            
            String pemKey = keyContent.toString()
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            
            byte[] keyBytes = Base64.getDecoder().decode(pemKey);
            
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(keyBytes));
        }
    }
    
    /**
     * Creates a hostname verifier that accepts all hostnames
     * WARNING: Use only for testing/development
     */
    public static HostnameVerifier createAcceptAllHostnameVerifier() {
        return (hostname, session) -> {
            logger.warn("Accepting hostname: {} without verification", hostname);
            return true;
        };
    }
    
    /**
     * Trust manager that accepts all certificates
     * WARNING: Use only for testing/development
     */
    private static class AcceptAllTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // Accept all client certificates
        }
        
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // Accept all server certificates
            logger.warn("Accepting server certificate without verification. Chain length: {}", chain.length);
        }
        
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
    
    /**
     * Creates a basic SSL context that accepts all certificates
     * WARNING: Use only for testing/development
     */
    public static SSLContext createAcceptAllSSLContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] { new AcceptAllTrustManager() }, new SecureRandom());
        logger.warn("Created SSL context that accepts all certificates. Not suitable for production!");
        return sslContext;
    }
}