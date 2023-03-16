# Traffic Shifting Demo

## Goal

To see how to split traffic between two version of the same microservice. Also can be
Used to fulfill the canary deployment Strategy.

### Procedure

1. First delete demo namespace and wait for the deletion to end
```shell
oc delete project demo
oc wait  --for=delete project/demo
```

2. Wait additional time (potentially) until you can recreate the namespace
```shell
oc new-project demo
```
3. Deploy versions v1 and v2 of the app-demo microservice to the demo namespace
```shell
oc apply -f ../../demo-application/openshift-manifests/v1/
oc apply -f ../../demo-application/openshift-manifests/v2/
```
4. Create A `DestinationRule` resource that will define the 2 versions of our microservice as two subsets, based on labels
```yaml
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: demo-app
  namespace: demo
spec:
  host: demo-app.demo.svc.cluster.local
  subsets:
    - name: v1
      labels:
        version: v1
    - name: v2
      labels:
        version: v2
```
```shell
oc apply -f destination-rule.yaml
```
5. Create a virtual service that will route all traffic to version v1:
```yaml
kind: VirtualService
apiVersion: networking.istio.io/v1alpha3
metadata:
  name: demo-app-mono
  namespace: demo
spec:
  hosts:
    - demo-app.demo.svc.cluster.local
  http:
    - route:
        - destination:
            host: demo-app.demo.svc.cluster.local
            subset: v1
          weight: 100
        - destination:
            host: demo-app.demo.svc.cluster.local
            subset: v2
```
```shell
oc apply -f virtual-service-mono-traffic.yaml
```

6. invoke 10 times the  demo-app service and print the response body output nicely:
```shell
 for i in {1..10}; do echo -n "attempt $i: "  ;  oc exec rest-api-client -n demo  -- curl -s  http://demo-app:8080/hello | jq .; echo ; done
```
Output:
```shell
attempt 1: {
  "from": "The Greeting Application Version= v1",
  "type": "General",
  "greeting": "Hello There"
}

attempt 2: {
  "from": "The Greeting Application Version= v1",
  "type": "General",
  "greeting": "Hello There"
}

attempt 3: {
  "from": "The Greeting Application Version= v1",
  "type": "General",
  "greeting": "Hello There"
}

attempt 4: {
  "from": "The Greeting Application Version= v1",
  "type": "General",
  "greeting": "Hello There"
}

attempt 5: {
  "from": "The Greeting Application Version= v1",
  "type": "General",
  "greeting": "Hello There"
}

attempt 6: {
  "from": "The Greeting Application Version= v1",
  "type": "General",
  "greeting": "Hello There"
}

attempt 7: {
  "from": "The Greeting Application Version= v1",
  "type": "General",
  "greeting": "Hello There"
}

attempt 8: {
  "from": "The Greeting Application Version= v1",
  "type": "General",
  "greeting": "Hello There"
}

attempt 9: {
  "from": "The Greeting Application Version= v1",
  "type": "General",
  "greeting": "Hello There"
}

attempt 10: {
  "from": "The Greeting Application Version= v1",
  "type": "General",
  "greeting": "Hello There"
}

```
7. Open Kiali UI and authenticate using Openshift Credentials:
```shell
 xdg-open https://$(oc get route kiali -n istio-system -o=jsonpath="{.spec.host}")
``` 
8. Click on the Graph menu on the left side, and choose the demo namespace, Look the graph of traffic propagation.
9. Delete the "mono" `VirtualService`:
```shell
oc delete -f virtual-service-mono-traffic.yaml
```

10. Now create `VirtualService` that splits the traffic 50%-50%:
```shell
oc apply -f virtual-service-shifting-traffic.yaml
```

11. Wait 5-10 seconds, and run loop of 10 invocations of the demo-app microservice:
```shell
for i in {1..10}; do echo -n "attempt $i: "  ;  oc exec rest-api-client -n demo  -- curl -s  http://demo-app:8080/hello | jq .; echo ; done
```
Output:
```shell
attempt 1: {
  "from": "The Greeting Application Version= v2",
  "to": "John Doe",
  "type": "General",
  "greeting": "Wish you prosperity and Wealth!"
}

attempt 2: {
  "from": "The Greeting Application Version= v1",
  "type": "General",
  "greeting": "Hello There"
}

attempt 3: {
  "from": "The Greeting Application Version= v2",
  "to": "John Doe",
  "type": "General",
  "greeting": "You're The Best!"
}

attempt 4: {
  "from": "The Greeting Application Version= v2",
  "to": "John Doe",
  "type": "General",
  "greeting": "Good Luck!!"
}

attempt 5: {
  "from": "The Greeting Application Version= v1",
  "type": "General",
  "greeting": "Hello There"
}

attempt 6: {
  "from": "The Greeting Application Version= v2",
  "to": "John Doe",
  "type": "General",
  "greeting": "Wish you Happy Resting!"
}

attempt 7: {
  "from": "The Greeting Application Version= v1",
  "type": "General",
  "greeting": "Hello There"
}

attempt 8: {
  "from": "The Greeting Application Version= v1",
  "type": "General",
  "greeting": "Hello There"
}

attempt 9: {
  "from": "The Greeting Application Version= v2",
  "to": "John Doe",
  "type": "General",
  "greeting": "Wish you a Joyful Day"
}

attempt 10: {
  "from": "The Greeting Application Version= v2",
  "to": "John Doe",
  "type": "General",
  "greeting": "Have a Shinny Day!"
}

```
12. Refresh the screen at kiali graph visualization , and you should see that the traffic that sent only to versio v1, now splitted between v1 and v2.
