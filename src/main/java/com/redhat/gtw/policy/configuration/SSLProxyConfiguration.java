package com.redhat.gtw.policy.configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SSLProxyConfiguration {

    @Value("${proxy.keystore.dest}")
    private String keystoreDest;
    @Value("${proxy.keystore.pass}")
    private String keystorePass;
    @Value("${proxy.consumer}")
    private String consumer;
    @Value("${proxy.producer}")
    private String producer;
    @Value("${proxy.producer-query}")
    private String producerQuery;
    @Value("${proxy.port}")
    private String port;

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

    public String getConsumer() {
        return consumer;
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getProducerQuery() {
        return producerQuery;
    }

    public void setProducerQuery(String producerQuery) {
        this.producerQuery = producerQuery;
    }
}
