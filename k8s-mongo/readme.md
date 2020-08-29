
Purpose of this k8s-mongo is to deploy a local mongo replicaset inorder to better assess the mongo APIs (Change Streams and CRUD). We use bitnami helm charts for development use.  

1. Add bitnami repo to helm:

	`helm repo add bitnami https://charts.bitnami.com/bitnami`

2. Create mongodb namespace:

	`helm create ns mongodb`

3. Install mongo replicaset `rs0` with a `NodePort` to interact with mongo without an ingress in dev. Additionally create a `test` database user, database and password for quick bootstrap:

	```
	helm install mongodb-release bitnami/mongodb -n mongodb --set service.type=NodePort --set 				service.nodePort=32017 --set replicaSet.enabled=true --set replicaSet.name=rs0 --set persistence.size=2Gi --set mongodbUsername=test,mongodbPassword=test,mongodbDatabase=test
	```