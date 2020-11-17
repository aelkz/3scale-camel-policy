package com.redhat.gtw.policy.configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SSLProxyConfiguration {

    @Value("${proxy.keystore.dest}")
    private String keystoreDest;
    @Value("${proxy.keystore.pass}")
    private String keystorePass;

    public String getKeystoreDest() {
        return keystoreDest;
    }

    public void setKeystoreDest(String keystoreDest) {
        this.keystoreDest = keystoreDest;
    }

    public String getKeystorePass() {
        return keystorePass;
    }

    public void setKeystorePass(String keystorePass) {
        this.keystorePass = keystorePass;
    }
}
