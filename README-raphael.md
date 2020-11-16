https://gitlab.consulting.redhat.com/consulting-brazil/banco-do-nordeste/3scale-custom-policies/ip-rate-limit/-/tree/spring-boot


https://access.redhat.com/documentation/en-us/red_hat_3scale_api_management/2.9/html-single/administering_the_api_gateway/index#camel-proxy-policy

https://catalog.redhat.com/software/containers/redhat-openjdk-18/openjdk18-openshift/58ada5701fbe981673cd6b10?container-tabs=dockerfile
https://catalog.redhat.com/software/containers/fuse7/fuse-java-openshift/5aba4fbb29373834f089f074?container-tabs=dockerfile


```
curl -k -vvv -x GET https://api.github.com/users/aelkz/repos --proxy http://127.0.0.1:8080/proxy
```
https://www.iditect.com/how-to/54478310.html
https://www.catshout.de/?p=161



KEYSTORE_PASSWORD=$(openssl rand -base64 512 | tr -dc A-Z-a-z-0-9 | head -c 25)

keytool -genkeypair -keyalg RSA -keysize 2048 -dname "CN=0.0.0.0" -alias https-key -keystore keystore.jks -storepass ${KEYSTORE_PASSWORD}
keytool -certreq -keyalg rsa -alias https-key -keystore keystore.jks -file camel.csr -storepass ${KEYSTORE_PASSWORD}
echo ${KEYSTORE_PASSWORD}


http://zetcode.com/springboot/https/
https://www.baeldung.com/spring-boot-https-self-signed-certificate
https://stackoverflow.com/questions/5706166/apache-camel-http-and-ssl
https://stackoverflow.com/questions/29687237/how-to-configure-camel-jetty-producer-endpoint-to-use-ssl-certificate


```
KEYSTORE_PASSWORD=$(openssl rand -base64 512 | tr -dc A-Z-a-z-0-9 | head -c 25)

# cria keystore da app
keytool -genkeypair -keyalg RSA -keysize 2048 -dname "CN=0.0.0.0" -alias camel-https-key -keystore keystore.jks -storepass ${KEYSTORE_PASSWORD}
# cria o csr para geração de um novo certificado
keytool -certreq -keyalg rsa -alias camel-https-key -keystore keystore.jks -file camel.csr -storepass ${KEYSTORE_PASSWORD}
# cria o certificado a partir do csr com assinatura da CA do BNB (precisa da key da CA)
openssl x509 -req -CA ca.crt -CAkey ca.key -in camel.csr -out camel.crt -days 3650 -CAcreateserial -passin pass:${KEYSTORE_PASSWORD}
# importa o certificado da CA (ca.crt) na keystore
keytool -import -file ca.crt -alias bnb.ca -keystore keystore.jks -storepass ${KEYSTORE_PASSWORD}
# importa o certificado auto-assinado camel para a keystore
keytool -import -file camel.crt -alias camel-https-key -keystore keystore.jks -storepass ${KEYSTORE_PASSWORD}
# keystore criada com o certificado camel + certificado da CA
echo ${KEYSTORE_PASSWORD}
```

https://support.code42.com/Administrator/6/Configuring/Install_a_CA-signed_SSL_certificate_for_HTTPS_console_access
