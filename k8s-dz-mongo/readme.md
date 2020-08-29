
The purpose of k8s-dz-mongo and k8s-dz-mongo-connector is to deploy a `KafkaConnect` cluster with `debezium-connector-mongo` plugins and `KafkaConnector` for mongo collection respectively.

1. Build a docker image for the `KafkaConnect`:  

	`eval $(minikube docker-env)`
	`docker build . -t debezium-mongo-connect:0.1`

2. Create `secret` from file containing `mongo_username` and `mongo_password` in kafka namespace:
  
	`kubectl -n kafka create secret generic mongo-credentials --from-file=mongo-credentials.properties`

3. To get the root password for dev:
	```
	kubectl get secret --namespace mongodb mongodb-release -o jsonpath="{.data.mongodb-root-password}" | base64 --decode
	```

4. Create `KafkaConnect` cluster with 1 replica:
	```
	kubectl -n kafka apply -f dz-mongo-connect.yaml
	kafkaconnect.kafka.strimzi.io/debezium-mongo-kafka-connect-cluster created

	kubectl -n kafka get kafkaconnect
	NAME DESIRED REPLICAS
	debezium-mongo-kafka-connect-cluster 1

	kubectl -n kafka get pods
	NAME READY STATUS RESTARTS AGE
	debezium-mongo-kafka-connect-cluster-connect-7bcbc6bb7-4h8wt 0/1 Running 0 20s
	```

5. Create a specific `KafkaConnector` for `orders` collection in `test` database:
  

	```
	kubectl -n kafka apply -f ../k8s-dz-mongo-connector/dz-mongo-connector.yaml
	kubectl -n kafka get kafkaconnector
	NAME AGE
	debezium-orders-connector 39s
	```