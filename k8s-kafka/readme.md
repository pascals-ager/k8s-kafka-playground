
The purpose of k8s-kafka is to deploy Apache Kafka using `strimzi` operator for evaluation.

1. If working with minikube, ignore this step. Set context for the gcloud cluster created in `/gcloud-cluster` directory.

```shell
kubectl config current-context
gke_ct-data-analytics-staging_europe-west1-d_ct-da-staging
```

2. Add strimzi to the helm repo:

`helm repo add strimzi https://strimzi.io/charts/`

3. Create `kafka` namespace:

`kubectl create ns kafka`

4. Install `strimzi-kafka-operator` with chart version `0.17.0`:

```shell
helm install -n kafka strimzi/strimzi-kafka-operator --version 0.17.0 --name-template strimzi-operator
```

5. Deploy `Kafka` custom resource with 3 replicas of `kafka + tls-sidecar` and `zookeeper + tls-sidecar`:

`kubectl -n kafka apply -f kafka-staging.yaml`

If working with minikube, use `kafka-dev.yaml`

6. This might take a while. Wait till the pods and services are running:

```shell
kubectl -n kafka get pods
NAME                                             READY   STATUS    RESTARTS   AGE
kafka-cluster-entity-operator-5857747dcf-fz5tj   3/3     Running   0          30h
kafka-cluster-kafka-0                            2/2     Running   0          30h
kafka-cluster-kafka-1                            2/2     Running   0          30h
kafka-cluster-kafka-2                            2/2     Running   0          30h
kafka-cluster-kafka-exporter-6c8749d9bf-nqbwl    1/1     Running   0          30h
kafka-cluster-zookeeper-0                        2/2     Running   0          30h
kafka-cluster-zookeeper-1                        2/2     Running   0          30h
kafka-cluster-zookeeper-2                        2/2     Running   0          30h
strimzi-cluster-operator-59b99fc7cf-drjqv        1/1     Running   2          31h


kubectl -n kafka get svc
NAME                                     TYPE           CLUSTER-IP     EXTERNAL-IP   PORT(S)                               AGE
kafka-cluster-kafka-0                    LoadBalancer   10.87.13.216   10.132.0.37   9094:32194/TCP                        30h
kafka-cluster-kafka-1                    LoadBalancer   10.87.6.166    10.132.0.35   9094:31971/TCP                        30h
kafka-cluster-kafka-2                    LoadBalancer   10.87.10.10    10.132.0.36   9094:31118/TCP                        30h
kafka-cluster-kafka-bootstrap            ClusterIP      10.87.1.116    <none>        9091/TCP,9092/TCP,9093/TCP,9404/TCP   30h
kafka-cluster-kafka-brokers              ClusterIP      None           <none>        9091/TCP,9092/TCP,9093/TCP            30h
kafka-cluster-kafka-exporter             ClusterIP      10.87.5.127    <none>        9404/TCP                              30h
kafka-cluster-kafka-external-bootstrap   LoadBalancer   10.87.14.66    10.132.0.34   9094:30577/TCP                        30h
kafka-cluster-zookeeper-client           ClusterIP      10.87.5.76     <none>        9404/TCP,2181/TCP                     30h
kafka-cluster-zookeeper-nodes            ClusterIP      None           <none>        2181/TCP,2888/TCP,3888/TCP            30h
```

7. Check if relevant DNS records are created (Not relevant for minikube):

```shell
gcloud dns record-sets list --zone=$DNS_MANAGED_ZONE
NAME                                              TYPE  TTL    DATA
external.da.ct.com.                      NS    21600  ns-cloud-d1.googledomains.com.,ns-cloud-d2.googledomains.com.,ns-cloud-d3.googledomains.com.,ns-cloud-d4.googledomains.com.
external.da.ct.com.                      SOA   21600  ns-cloud-d1.googledomains.com. cloud-dns-hostmaster.google.com. 1 21600 3600 259200 300
kafka-bootstrap-stg.external.da.ct.com.  A     60     10.132.0.34
kafka-bootstrap-stg.external.da.ct.com.  TXT   300    "heritage=external-dns,external-dns/owner=da-user,external-dns/resource=service/kafka/kafka-cluster-kafka-external-bootstrap"
kafka-broker-0-stg.external.da.ct.com.   A     60     10.132.0.37
kafka-broker-0-stg.external.da.ct.com.   TXT   300    "heritage=external-dns,external-dns/owner=da-user,external-dns/resource=service/kafka/kafka-cluster-kafka-0"
kafka-broker-1-stg.external.da.ct.com.   A     60     10.132.0.35
kafka-broker-1-stg.external.da.ct.com.   TXT   300    "heritage=external-dns,external-dns/owner=da-user,external-dns/resource=service/kafka/kafka-cluster-kafka-1"
kafka-broker-2-stg.external.da.ct.com.   A     60     10.132.0.36
kafka-broker-2-stg.external.da.ct.com.   TXT   300    "heritage=external-dns,external-dns/owner=da-user,external-dns/resource=service/kafka/kafka-cluster-kafka-2"
```

Note: Simply toggle `tls: true` on line 24 of `kafka-staging.yaml` to enable `tls` for external clients. Doing so implies that external clients would require tls credentials to access the cluster.

Following steps are useful if mutual tls authentication is enabled for external listeners (aka external access to the kafka cluster).

 - Create a `KafkaTopic`
```shell
kubectl -n kafka apply -f test-topic.yaml
```

 - Create a `KafkaUser` with ACL to read and write from the topic.
```shell
kubectl -n kafka apply -f test-user.yaml
```

 - Examine the certificates and keys available for the user.
```shell
kubectl -n kafka get secrets test-user -o yaml
```
From an external clients perspective, there are two steps:

 -  Server Verification: Establish the identity of the Server (Kafka
   in this case) during the SSL handshake. For this, the client needs to
   have the Public Key of the CA which is used to sign the broker
   certificates: `ca.crt`. This key must be used to create a
   `truststore`. This performs both Server authentication and on-wire
   encryption of the data.

```shell
kubectl -n kafka get secrets kafka-cluster-cluster-ca-cert -o jsonpath="{.data.ca\\.crt}" | base64 -d > ca.crt
keytool -import -file ca.crt -keystore ${desired_truststore_location}/client.truststore.p12 -alias ca -storepass ${password_for_truststore} -noprompt
```

 - Client Authentication: Establish the identity of the Client
   (producer/consumer). `user.key` and 'user.crt' (combined to give
   `user.p12`) contain the key-pairs generated for the client called
   `test-user` in this example. The pair must be used to creat a
   `keystore`. This takes care of authenticating the client at the
   Server.

```shell
# Get the key pairs
kubectl -n kafka get secrets test-user -o jsonpath="{.data.user\\.crt}" | base64 -d > user.crt
kubectl -n kafka get secrets test-user -o jsonpath="{.data.user\\.key}" | base64 -d > user.key
# Create a .p12 file using the key-pairs
openssl pkcs12 -export -in user.crt -inkey user.key -name user.p12 -password pass:${p12_password} -out user.p12
# Create a keystore from the .p12 files
keytool -importkeystore -alias user.p12 -deststorepass ${password_for_keystore} -destkeystore ${desired_keysstore_location}/user.keystore.p12 -srcstorepass ${p12_password} -srckeystore user.p12 -srcstoretype PKCS12 -deststoretype PKCS12
```

11. The client can use the `truststore` and `keystore` to access the cluster.
```shell
./bin/kafka-console-consumer.sh --bootstrap-server kafka-bootstrap-stg.external.da.ct.com:9094 --topic test-topic
--consumer-property security.protocol=SSL
--consumer-property ssl.truststore.type=PKCS12
--consumer-property ssl.keystore.type=PKCS12
--consumer-property ssl.truststore.password=${password_for_truststore}
--consumer-property ssl.keystore.password=${password_for_keystore}
--consumer-property group.id=test-group
--consumer-property ssl.truststore.location=${desired_truststore_location}/client.truststore.p12
--consumer-property ssl.keystore.location=${desired_keysstore_location}/user.keystore.p12 --from-beginning
```
Note: To debug SSL issues, use `export KAFKA_OPTS="-Djavax.net.debug=ssl"`
Note: The certificates and keys uploaded in this folder are no longer valid. 