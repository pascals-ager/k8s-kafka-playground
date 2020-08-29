1. Create a Google Cloud Cluster with `clouddns` api enabled. For demo purposes, we create a cluster with `clouddns.readwrite` scope applied to all pods. To restrict the scope to specific pods, service account might be created with `dns.admin` role. [See](https://knative.dev/docs/serving/using-external-dns-on-gcp/) for details.

```shell
export PROJECT_NAME=pg-data-staging
export CUSTOM_DOMAIN=external.data.pg.com
export CLUSTER_NAME=pg-data-staging
export CLUSTER_ZONE=europe-west1-d
export DNS_MANAGED_ZONE=external-data-pg-zone


gcloud beta container clusters create $CLUSTER_NAME \
    --zone=$CLUSTER_ZONE \
    --release-channel=regular \
    --machine-type=n1-standard-4 \
    --enable-ip-alias \
    --enable-autoscaling --min-nodes=1 --max-nodes=10 \
    --enable-autorepair \
    --scopes=service-control,service-management,compute-rw,storage-ro,cloud-platform,logging-write,monitoring-write,datastore,"https://www.googleapis.com/auth/ndev.clouddns.readwrite" \
    --num-nodes=5
```

2. Create a DNS Zone for managing DNS records in the project.
```shell
gcloud dns managed-zones create $DNS_MANAGED_ZONE \
    --dns-name $CUSTOM_DOMAIN \
    --description "Automatically managed zone by kubernetes.io/external-dns"
```

3. Deploy ExternalDNS in the `default` namespace.
```shell
kubectl apply -f external-dns.yaml
```