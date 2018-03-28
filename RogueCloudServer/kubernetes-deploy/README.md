
## Deployment to Kubernetes Configuration Instructions



### Login to cluster 

```
bx login

bx cs region-set us-east
# bx cs region-set us-south

bx cs clusters
bx cs cluster-config <cluster_name_or_id>

bx cr login
bx cr namespace-list
```



### Open Kubernetes Proxy

Extract the token, then start the proxy:
```
kubectl config view -o jsonpath='{.users[0].user.auth-provider.config.id-token}'
kubectl proxy 
```

Next, visit: http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/


### Tag and push a build to IBM Containers Private Registry


```
docker build -t rogue-cloud .
docker tag rogue-cloud:latest registry.ng.bluemix.net/rogue-cloud-repo/rogue-cloud:latest
docker push registry.ng.bluemix.net/rogue-cloud-repo/rogue-cloud:latest
```


###  Deploy

#### Using Load Balancer
```
kubectl apply -f rogue-cloud.server.yaml
kubectl apply -f load-balancer.yaml
```

#### Using NodePort service
```
kubectl expose deployment/rogue-cloud-server --type=NodePort --port=29080 --name=rogue-cloud-server --target-port=29080
```
