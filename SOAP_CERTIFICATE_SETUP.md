# SOAP Client Certificate Authentication Setup

This guide explains how to add certificate authorization for SOAP requests in Java, providing a secure way for other teams to consume your services.

## Overview

The solution provides multiple approaches for certificate-based authentication:

1. **Keystore/Truststore files** (PKCS12, JKS formats)
2. **PEM certificate and key files**
3. **Apache CXF** for advanced configuration
4. **JAX-WS** for standard web services

## Quick Start

### 1. Add Dependencies

The project includes all necessary dependencies in `pom.xml`:
- Jakarta XML WS API
- Apache CXF
- HTTP Client 5
- SSL/TLS support

Run `mvn install` to download dependencies.

### 2. Basic Usage Example

```java
// Using keystore files
ExampleSoapService client = new SecureSOAPClient.Builder()
    .keystoreConfig("/path/to/client-cert.p12", "password", "PKCS12")
    .truststoreConfig("/path/to/truststore.jks", "trustpass")
    .buildCxfClient(ExampleSoapService.class, "https://service.example.com/soap");

// Make SOAP call
String response = client.processRequest("Your request data");
```

## Certificate Formats Supported

### 1. PKCS#12 Keystore (.p12, .pfx)
```java
SecureSOAPClient.Builder()
    .keystoreConfig("/path/to/cert.p12", "password", "PKCS12")
    .buildCxfClient(ServiceInterface.class, "https://endpoint");
```

### 2. JKS Keystore (.jks)
```java
SecureSOAPClient.Builder()
    .keystoreConfig("/path/to/cert.jks", "password", "JKS")
    .buildCxfClient(ServiceInterface.class, "https://endpoint");
```

### 3. PEM Certificate Files
```java
SecureSOAPClient.Builder()
    .pemConfig(
        "/path/to/client-cert.pem",    // Client certificate
        "/path/to/client-key.pem",     // Private key
        "keyPassword",                  // Key password (optional)
        "/path/to/ca-cert.pem"         // CA certificate (optional)
    )
    .buildCxfClient(ServiceInterface.class, "https://endpoint");
```

## Implementation Approaches

### 1. Apache CXF (Recommended)
```java
ExampleService client = SecureSOAPClient.createCxfClient(
    ExampleService.class,
    "https://service.example.com/soap",
    certificateConfig
);
```

**Advantages:**
- Better SSL configuration control
- Advanced security features
- Easier debugging
- Better performance

### 2. JAX-WS
```java
URL wsdlLocation = new URL("https://service.example.com/soap?wsdl");
ExampleService client = SecureSOAPClient.createJaxWsClient(
    ExampleService.class,
    wsdlLocation,
    "https://service.example.com/soap",
    certificateConfig
);
```

**Advantages:**
- Standard Java EE approach
- Better tooling support
- WSDL-first development

## Certificate Setup Instructions

### Step 1: Obtain Certificates

You'll need:
1. **Client Certificate**: Identifies your application to the server
2. **Private Key**: Proves ownership of the client certificate
3. **CA Certificate**: Validates the server's certificate (optional if using system truststore)

### Step 2: Convert Certificates (if needed)

#### Convert PEM to PKCS12:
```bash
openssl pkcs12 -export -in client-cert.pem -inkey client-key.pem -out client-cert.p12 -name "client"
```

#### Convert PKCS12 to JKS:
```bash
keytool -importkeystore -srckeystore client-cert.p12 -srcstoretype PKCS12 -destkeystore client-cert.jks -deststoretype JKS
```

#### Create Truststore with CA Certificate:
```bash
keytool -import -alias ca -file ca-cert.pem -keystore truststore.jks -storepass trustpass
```

### Step 3: Configure Application

Update `src/main/resources/soap-client.properties`:

```properties
# Keystore configuration
soap.client.keystore.path=/path/to/client-cert.p12
soap.client.keystore.password=your-password
soap.client.keystore.type=PKCS12

# Truststore configuration
soap.client.truststore.path=/path/to/truststore.jks
soap.client.truststore.password=trustpass

# Service endpoint
soap.service.endpoint.url=https://partner-service.example.com/soap
```

## Security Best Practices

### 1. Certificate Storage
- Store certificates in secure locations with restricted access
- Use environment variables or secure vaults for passwords
- Regularly rotate certificates before expiration

### 2. Password Management
```java
// Use environment variables
String keystorePassword = System.getenv("KEYSTORE_PASSWORD");

// Or use secure configuration management
String keystorePassword = secureConfig.get("soap.client.keystore.password");
```

### 3. Certificate Validation
```java
// Production: Always validate certificates
.truststoreConfig("/path/to/production-truststore.jks", "password")

// Development only: Accept all (NOT for production!)
.acceptAllCertificates(true)  // WARNING: Development only!
```

## Troubleshooting

### Common Issues

#### 1. Certificate Not Found
```
Error: java.security.UnrecoverableKeyException
```
**Solution:** Check keystore path and password

#### 2. SSL Handshake Failure
```
Error: javax.net.ssl.SSLHandshakeException
```
**Solutions:**
- Verify certificate validity dates
- Check if CA certificate is in truststore
- Ensure TLS protocol compatibility

#### 3. Hostname Verification Failed
```
Error: javax.net.ssl.SSLPeerUnverifiedException
```
**Solutions:**
- Add hostname to certificate SAN
- Use `.acceptAllHostnames(true)` for testing only

### Debug SSL Issues

Enable SSL debugging:
```java
System.setProperty("javax.net.debug", "ssl,handshake");
```

Or use SSL logging in your application:
```java
logger.debug("SSL Context created with keystore: {}", keystorePath);
```

## Integration Examples

### With Gatling Tests
```java
// Create SOAP client for performance testing
ExampleService soapClient = new SecureSOAPClient.Builder()
    .keystoreConfig("/certs/test-client.p12", "testpass", "PKCS12")
    .buildCxfClient(ExampleService.class, "https://test-env/soap");

// Use in Gatling scenario
exec(session -> {
    String response = soapClient.processRequest("test-data");
    return session.set("soap_response", response);
});
```

### With Spring Boot
```java
@Configuration
public class SoapClientConfig {
    
    @Value("${soap.keystore.path}")
    private String keystorePath;
    
    @Bean
    public ExampleService soapClient() throws Exception {
        return new SecureSOAPClient.Builder()
            .keystoreConfig(keystorePath, keystorePassword, "PKCS12")
            .buildCxfClient(ExampleService.class, endpointUrl);
    }
}
```

## Certificate Lifecycle Management

### 1. Certificate Monitoring
```java
// Check certificate expiration
X509Certificate cert = loadCertificateFromKeystore();
Date expiration = cert.getNotAfter();
if (expiration.before(new Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000))) {
    logger.warn("Certificate expires soon: {}", expiration);
}
```

### 2. Certificate Renewal
- Set up monitoring for certificate expiration
- Implement automated certificate renewal where possible
- Test certificate replacement in staging environments

### 3. Backup and Recovery
- Maintain secure backups of certificates and keys
- Document certificate recovery procedures
- Test recovery processes regularly

## Production Deployment Checklist

- [ ] Certificates are valid and not expiring soon
- [ ] Passwords are stored securely (not in code)
- [ ] Truststore contains only necessary CA certificates
- [ ] SSL debugging is disabled
- [ ] Certificate paths are correct for production environment
- [ ] Hostname verification is enabled
- [ ] Connection timeouts are configured appropriately
- [ ] Logging is configured for monitoring (but not debugging)
- [ ] Certificate expiration monitoring is in place

## Support and Resources

- See `SOAPClientExamples.java` for complete working examples
- Check `CertificateManager.java` for low-level certificate operations
- Refer to Apache CXF documentation for advanced configuration
- Use SSL debugging for troubleshooting connection issues

For questions or issues, contact the development team.