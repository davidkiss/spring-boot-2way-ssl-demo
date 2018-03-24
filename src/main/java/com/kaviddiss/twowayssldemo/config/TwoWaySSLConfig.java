package com.kaviddiss.twowayssldemo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import org.apache.commons.ssl.KeyMaterial;
import org.springframework.util.StringUtils;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * Configures <code>SSLContext</code> with a keystore and truststore from either classpath or filesystem.
 * See https://github.com/davidkiss/spring-boot-2way-ssl-demo
 * @author davidk
 */
@Configuration
@ConditionalOnProperty("ssl.enable2way")
public class TwoWaySSLConfig {
    private static final Logger LOG = LoggerFactory.getLogger(TwoWaySSLConfig.class);

    @Bean
    public SSLContext create2WaySSLContext(@Value("${ssl.overrideDefault:true}") boolean overrideDefault,
                                  @Value("${ssl.keystore.path}") String keystorePath,
                                  @Value("${ssl.keystore.password}") String keystorePassword,
                                  @Value("${ssl.truststore.path:}") String truststorePath,
                                  @Value("${ssl.truststore.password:}") String truststorePassword) throws Exception
    {
        LOG.info("Configuring 2-way SSL with keystore: '{}' and truststore: '{}'", keystorePath, truststorePath);
        KeyStore keyStore = createKeyStore(keystorePath, keystorePassword);
        KeyManager[] keyManagers = createKeyManagers(keyStore, keystorePassword);

        TrustManager[] trustmanagers = StringUtils.isEmpty(truststorePath)
                ? new TrustManager[]{ createTrustAllTrustManager() }
                : createTrustManagerFactory(truststorePath, truststorePassword).getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustmanagers, new SecureRandom());

        if (overrideDefault) {
            SSLContext.setDefault(sslContext);
        }

        LOG.info("Configured 2-way SSL");
        return sslContext;
    }

    private static TrustManagerFactory createTrustManagerFactory(String truststorePath, String truststorePassword) throws GeneralSecurityException, IOException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore truststore = createKeyStore(truststorePath, truststorePassword);
        tmf.init(truststore);
        return tmf;
    }

    private static KeyStore _createDefaultKeyStore(String keystorePath, String keystorePassword) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream inputStream = getKeyStoreInputStream(keystorePath);
        keyStore.load(inputStream, keystorePassword.toCharArray());
        return keyStore;
    }

    private static KeyStore _createKeyMaterialKeyStore(String keystorePath, String keystorePassword) throws GeneralSecurityException, IOException {
        InputStream keystoreLocation = getKeyStoreInputStream(keystorePath);

        return new KeyMaterial(keystoreLocation, keystorePassword.toCharArray()).getKeyStore();
    }

    private static KeyStore createKeyStore(String keystorePath, String keystorePassword) throws GeneralSecurityException, IOException {
        KeyStore keyStore;
        try {
            keyStore = _createDefaultKeyStore(keystorePath, keystorePassword);
        } catch (Exception e) {
            // Using <code>_createDefaultKeyStore()</code> with a PKCS12 keystore may result in a ConcurrentModifiationException,
            // in order to avoid that, we're using the ca.juliusdavies:not-yet-commons-ssl library here.
            // See https://www.tbs-certificates.co.uk/FAQ/en/626.html
            keyStore = _createKeyMaterialKeyStore(keystorePath, keystorePassword);
        }

        logKeystore(keyStore, keystorePath);

        return keyStore;
    }

    private static void logKeystore(KeyStore keystore, String keystorePath) throws KeyStoreException {
        LOG.info("Keystore '{}' has {} entries", keystorePath, keystore.size());
        for (Enumeration<String> e = keystore.aliases(); e.hasMoreElements(); ) {
            LOG.info("Keystore alias: {}", e.nextElement());
        }
    }

    private static KeyManager[] createKeyManagers(KeyStore keystore, String keystorePassword) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmfactory.init(keystore, keystorePassword.toCharArray());
        return kmfactory.getKeyManagers();
    }

    private static InputStream getKeyStoreInputStream(String keystorePath) throws FileNotFoundException {
        File keystoreFile = new File(keystorePath);
        InputStream keystoreLocation = keystoreFile.exists() && keystoreFile.isFile()
                ? new FileInputStream(keystoreFile)
                : TwoWaySSLConfig.class.getResourceAsStream(keystorePath);

        if (keystoreLocation == null) {
            throw new IllegalStateException("No keystore found at " + keystorePath);
        }
        return keystoreLocation;
    }

    private static X509TrustManager createTrustAllTrustManager() {
        return new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        };
    }}
