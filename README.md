# 3scale camel-policy

### APPLICATION DEFAULT ENDPOINTS

| Method | URL | Description |
| ------ | --- | ----------- |
| GET,PUT,POST,DELETE,PATCH | <container-exposed> http://0.0.0.0:8443 | Proxy Endpoint (consumer) |

### KEYSTORE SETUP

```
sudo mkdir /tls
sudo chown -R raphael: /tls

KEYSTORE_PASSWORD=$(openssl rand -base64 512 | tr -dc A-Z-a-z-0-9 | head -c 25)
keytool -genkeypair -keyalg RSA -keysize 2048 -dname "CN=0.0.0.0" -alias https-key -keystore keystore.jks -storepass ${KEYSTORE_PASSWORD}
echo ${KEYSTORE_PASSWORD}

```

### TESTING (WORKING EXAMPLE)

```
curl -k -vvv "https://sample-production.apps.<host>.redhat.com/get" -H 'user_key: 38c6657cd5dee0a4a5c1dd5805dd482a'
```

PS. Need to setup a new 3scale account with a new application (user_key)

### 3SCALE AND OPENSHIFT CONFIGURATION STEPS

### [1] 3SCALE CONFIGURATION

1.1 Create a new Product with name `SAMPLE`

1.2 Create a new Backend and add it to `SAMPLE` product

<p align="center">
<img src="https://raw.githubusercontent.com/aelkz/3scale-camel-policy/https/_images/02.png" title="3scale sample backend" width="80%" height="80%" />
</p>

1.3 Define the policy chain as follows

<p align="center">
<img src="https://raw.githubusercontent.com/aelkz/3scale-camel-policy/https/_images/05.png" title="3scale policy chain" width="80%" height="80%" />
</p>

1.4 Add a new policy `camel-service` and configure it with your camel application (this proxy app)

<p align="center">
<img src="https://raw.githubusercontent.com/aelkz/3scale-camel-policy/https/_images/01.png" title="3scale policy: camel service" width="65%" height="65%" />
</p>

**PS**. ON `https_proxy` ATTRIBUTE YOU MUST USE *HTTP* SCHEMA NOT HTTPS. USE PORT 8443 (RECOMENDED)<br>
Further details on: https://issues.redhat.com/browse/THREESCALE-6394

1.5 Save all configurations and promote the proxy (Product) from **stage** to **production**.

### [2] OPENSHIFT CONFIGURATION

2.1 Deploy the this application
2.2 Create all service ports

```
export APP=policy-api

echo "
  apiVersion: v1
  kind: Service
  metadata:
    name: "${APP}"
    annotations:
      prometheus.io/scrape: \"true\"
      prometheus.io/port: \"9779\"
    labels:
      expose: \"true\"
      app: "${APP}"
  spec:
    ports:
      - name: proxy-http
        port: 8080
        protocol: TCP
        targetPort: 8080
      - name: proxy-https
        port: 8443
        protocol: TCP
        targetPort: 8443
      - name: http
        port: 8090
        protocol: TCP
        targetPort: 8090
      - name: metrics
        port: 8081
        protocol: TCP
        targetPort: 8081
    selector:
      app: "${APP}"
    type: ClusterIP
    sessionAffinity: \"None\"
" | oc create -f - -n ${PROJECT_NAMESPACE}
```

<p align="center">
<img src="https://raw.githubusercontent.com/aelkz/3scale-camel-policy/https/_images/04.png" title="3scale policy: service ports" width="100%" height="100%" />
</p>

2.3 Set application policy environment variables as follows:

<p align="center">
<img src="https://raw.githubusercontent.com/aelkz/3scale-camel-policy/https/_images/03.png" title="3scale policy: environment variables" width="100%" height="100%" />
</p>

```
keytool -genkeypair -keyalg RSA -keysize 2048 -dname "CN=0.0.0.0" -alias https-key -keystore keystore.jks -storepass ${KEYSTORE_PASSWORD}
echo ${KEYSTORE_PASSWORD}

oc rollout pause dc ${APP} -n ${PROJECT_NAMESPACE}
oc create configmap policy-api-keystore-config --from-file=./keystore.jks -n ${PROJECT_NAMESPACE}
oc set volume dc/${APP} --add --overwrite --name=policy-api-config-volume -m /deployments/data -t configmap --configmap-name=policy-api-keystore-config -n ${PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite OPENSHIFT_PROXY_KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD} -n ${PROJECT_NAMESPACE}

oc set env dc/${APP} --overwrite OPENSHIFT_PROXY_CONSUMER_COMPONENT=netty4-http -n ${PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite OPENSHIFT_PROXY_PRODUCER_COMPONENT=http4 -n ${PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite OPENSHIFT_PRODUCER_QUERY=?connectionClose=false&bridgeEndpoint=true&copyHeaders=true -n ${PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite OPENSHIFT_PROXY_PORT=8443 -n ${PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite OPENSHIFT_PROXY_KEYSTORE_PASSWORD=77z9SYEGhSovlsBkALpko0BUb -n ${PROJECT_NAMESPACE}

oc rollout resume dc ${APP} -n ${PROJECT_NAMESPACE}
```

PS. Set your keystore and keystore passphrase accordingly.
