
The repository tracks the development of a minikube CDC data playground.

The following components are evaluated:
1. `MongoDB:4.2` to model our production databases. (Manifests in `/k8s-mongo`)
2. `Apache Kafka` via [`strimzi-kafka-operator:v0.17.0`](https://github.com/strimzi/strimzi-kafka-operator/tree/0.17.0/helm-charts/strimzi-kafka-operator) as messaging backbone. (Manifests in `/k8s-kafka`)
3. [`Debezium MongoDB Kafka Connector`](https://debezium.io/documentation/reference/1.1/connectors/mongodb.html) to CDC Mongo order collections to Kafka topics. (Deprecated, since we have the choice of using the official connector)
4. [`Official MongoDB Kafka Connector`](https://docs.mongodb.com/kafka-connector/v1.0/kafka-installation/) to CDC Mongo order collections to Kafka topics. (Manifests available in `/k8s-mongo-connect` and `/k8s-mongo-connector`. Deploy the connect cluster before deploying the individual connectors.)
5. External DNS configuration for Google Cloud under `/gcloud-dns`
6. A Scala Application under `/monixMongoStream` to play with the reactive mongo `changestream` API using the awesome [`Monix`](https://monix.io/) and [`Cats`](https://typelevel.org/cats/) libraries.


Verify Kafka topics creation:

    ./kafka-topics.sh --bootstrap-server $(minikube ip):32100 --list
    __consumer_offsets
    connect-cluster-configs
    connect-cluster-offsets
    connect-cluster-status
    mongok.test.orders

Verify the Kafka Change stream:

    ./kafka-console-consumer.sh --bootstrap-server $(minikube ip):32100 --topic mongok.test.orders
    "{\"_id\": {\"_data\": \"825EC2CDBE000000012B022C0100296E5A10040059EDB100714961B3CC32C3E5CD271146645F696400645EC39A7DFEC359FD2D00E1CC0004\"}, \"operationType\": \"delete\", \"clusterTime\": {\"$timestamp\": {\"t\": 1589824958, \"i\": 1}}, \"ns\": {\"db\": \"test\", \"coll\": \"orders\"}, \"documentKey\": {\"_id\": {\"$oid\": \"5ec39a7dfec359fd2d00e1cc\"}}}"
    "{\"_id\": {\"_data\": \"825EC2CDC4000000012B022C0100296E5A10040059EDB100714961B3CC32C3E5CD271146645F696400645EC39B61FEC359FD2D00E1CD0004\"}, \"operationType\": \"insert\", \"clusterTime\": {\"$timestamp\": {\"t\": 1589824964, \"i\": 1}}, \"fullDocument\": {\"_id\": {\"$oid\": \"5ec39b61fec359fd2d00e1cd\"}, \"id\": \"ord2348\", \"version\": \"2\", \"createdAt\": \"2020-03-20T23:44:05.396+01:00\", \"lastModifiedAt\": \"2020-03-20T23:44:05.396+01:00\", \"orderNumber\": \"2345\", \"customerId\": \"cust456\", \"country\": \"DE\", \"orderState\": \"Open\", \"InventoryMode\": \"TrackOnly\", \"origin\": \"Customer\"}, \"ns\": {\"db\": \"test\", \"coll\": \"orders\"}, \"documentKey\": {\"_id\": {\"$oid\": \"5ec39b61fec359fd2d00e1cd\"}}}"
    "{\"_id\": {\"_data\": \"825EC2CDC9000000012B022C0100296E5A10040059EDB100714961B3CC32C3E5CD271146645F696400645EC39B61FEC359FD2D00E1CD0004\"}, \"operationType\": \"update\", \"clusterTime\": {\"$timestamp\": {\"t\": 1589824969, \"i\": 1}}, \"fullDocument\": {\"_id\": {\"$oid\": \"5ec39b61fec359fd2d00e1cd\"}, \"id\": \"ord2348\", \"version\": \"2\", \"createdAt\": \"2020-03-20T23:44:05.396+01:00\", \"lastModifiedAt\": \"2020-03-20T23:44:05.396+01:00\", \"orderNumber\": \"2345\", \"customerId\": \"cust456\", \"country\": \"DE\", \"orderState\": \"Confirmed\", \"InventoryMode\": \"TrackOnly\", \"origin\": \"Customer\"}, \"ns\": {\"db\": \"test\", \"coll\": \"orders\"}, \"documentKey\": {\"_id\": {\"$oid\": \"5ec39b61fec359fd2d00e1cd\"}}, \"updateDescription\": {\"updatedFields\": {\"orderState\": \"Confirmed\"}, \"removedFields\": []}}"
    