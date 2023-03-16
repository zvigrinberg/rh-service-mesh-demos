# Fault Injection

## Goal And Motivation

In case you want to test your application/microservices for resiliency and fault tolerance,
RH Service mesh provides mechanism of injecting faults into service , like delays and aborts, without changing the consumed services' application code.
Hence bringing to application that test themselves in front of these services ease of testing to achieve a robust and resilient application.

### Demo Procedure:

1. Delete any former policy resource
```shell
 oc delete virtualservices.networking.istio.io,destinationrule -l policyResource="true"
```

2. Create a Destination rule, which selecting to each subset the pods of the corresponding version, based on pods labels:
```shell
oc apply -f destination-rule-subseting.yaml
```
3. Create the `VirtualService` That will cause the envoy proxy to Inject delays and aborts to the consumed demo-app service.
```yaml
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: demo-app-abort-delay-injection
  namespace: demo
  labels:
    demoMeshResource: "true"
    policyResource: "true"
spec:
  hosts:
    - demo-app
  http:
    - route:
        - destination:
            host: demo-app.demo.svc.cluster.local
            subset: v1
      fault:
        abort:
          percentage:
            value: 50
            # Some 400<=HttpStatusCode
          httpStatus: 400
        delay:
          percentage:
            value: 20
          fixedDelay: 7s
```
```shell
oc apply -f virtual-service-inject-abort-delay.yaml
```

4. Run 20 times the service
```shell
for i in {1..20}; do echo -n "attempt $i: "  ;  oc exec rest-api-client -n demo  -- curl -i  http://demo-app:8080/hello ; echo ; done
```
Output:
```shell
attempt 1:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    89  100    89    0     0  22250      0 --:--:-- --:--:-- --:--:-- 22250
HTTP/1.1 200 OK
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 2
date: Wed, 15 Mar 2023 12:27:11 GMT
server: envoy

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}
attempt 2:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    18  100    18    0     0  18000      0 --:--:-- --:--:-- --:--:-- 18000
HTTP/1.1 400 Bad Request
content-length: 18
content-type: text/plain
date: Wed, 15 Mar 2023 12:27:13 GMT
server: envoy
connection: close

fault filter abort
attempt 3:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    89  100    89    0     0  HTTP/1.1 200 OK:--:-- --:--:-- --:--:--     0
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 3
date: Wed, 15 Mar 2023 12:27:15 GMT
server: envoy

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}22250      0 --:--:-- --:--:-- --:--:-- 22250

attempt 4:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    18  100    18    0     0  18000      0 --:--:-- --:--:-- --:--:-- 1HTTP/1.1 400 Bad Request
content-length: 18
content-type: text/plain
date: Wed, 15 Mar 2023 12:27:16 GMT
server: envoy
connection: close

fault filter abort8000

attempt 5:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    18  100    18    0     0  18000      0 --:--:-- --:--:-- --:-HTTP/1.1 400 Bad Request
content-length: 18
content-type: text/plain
date: Wed, 15 Mar 2023 12:27:18 GMT
server: envoy
connection: close

fault filter abort-:-- 18000

attempt 6:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    89  100    89    0     0  22250      0 --:--:-- --:--:-- --:--:-- 22250
HTTP/1.1 200 OK
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 2
date: Wed, 15 Mar 2023 12:27:20 GMT
server: envoy

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}
attempt 7:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    18  100    18    0     0  18000      0 --:--:-- --:--:-- --:--:-- 18000
HTTP/1.1 400 Bad Request
content-length: 18
content-type: text/plain
date: Wed, 15 Mar 2023 12:27:22 GMT
server: envoy
connection: close

fault filter abort
attempt 8:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    89  100    89    0     0  22250      0 --:--:-- --:--:-- --:--:-- 22250
HTTP/1.1 200 OK
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 3
date: Wed, 15 Mar 2023 12:27:23 GMT
server: envoy

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}
attempt 9:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    18  100    18    0     0  18000      0 --:--:-- --:--:-- --:--:-- 18000
HTTP/1.1 400 Bad Request
content-length: 18
content-type: text/plain
date: Wed, 15 Mar 2023 12:27:25 GMT
server: envoy
connection: close

fault filter abort
attempt 10:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    89  100    89    0     0     12      0  0:00:07  0:00:07 --:--:--    23
HTTP/1.1 200 OK
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 3
date: Wed, 15 Mar 2023 12:27:34 GMT
server: envoy

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}
attempt 11:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    18  100    18    0     0  18000      0 --:--:-- --:--:--HTTP/1.1 400 Bad Request
content-length: 18
content-type: text/plain
date: Wed, 15 Mar 2023 12:27:35 GMT
server: envoy
connection: close

fault filter abort --:--:-- 18000

attempt 12:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    89  100    89    0     0  22250      0 --:--:-- --:--:-- --:--:-- 22250
HTTP/1.1 200 OK
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 2
date: Wed, 15 Mar 2023 12:27:37 GMT
server: envoy

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}
attempt 13:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    18  100    18    0     0  18000      0 --:--:-- --:--:-- --:--:-- 18000
HTTP/1.1 400 Bad Request
content-length: 18
content-type: text/plain
date: Wed, 15 Mar 2023 12:27:39 GMT
server: envoy
connection: close

fault filter abort
attempt 14:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    18  100    18    0     0      2      0  0:00:09  0:00:07  0:00:02     3HTTP/1.1 400 Bad Request
content-length: 18
content-type: text/plain
date: Wed, 15 Mar 2023 12:27:48 GMT
server: envoy
connection: close

100    18  100    18    0     0      2      0  0:00:09  0:00:07  0:00:02     4

attempt 15:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0HTTP/1.1 400 Bad Request
content-length: 18
content-type: text/plain
date: Wed, 15 Mar 2023 12:27:49 GMT
server: envoy
connection: close

100    18  100    18    0     0  18000      0 --:--:-- --:--:-- --:--:-- 18000

attempt 16:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    89  100    89    0     0  HTTP/1.1 200 OK:--:-- --:--:-- --:--:--     0
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 3
date: Wed, 15 Mar 2023 12:27:51 GMT
server: envoy

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}17800      0 --:--:-- --:--:-- --:--:-- 17800

attempt 17:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    18  100    18    0     0  18000      0 --:--:-- --:--:-- --:--:-- 18000
HTTP/1.1 400 Bad Request
content-length: 18
content-type: text/plain
date: Wed, 15 Mar 2023 12:27:53 GMT
server: envoy
connection: close

fault filter abort
attempt 18:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    18  100    18    0     0  18000      0 --:--:-- --:--:-- --:--:-- 18000
HTTP/1.1 400 Bad Request
content-length: 18
content-type: text/plain
date: Wed, 15 Mar 2023 12:27:54 GMT
server: envoy
connection: close

fault filter abort
attempt 19:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    89  100    89    0     0  22250      0 --:--:-- --:--:-- --:--:-- 22250
HTTP/1.1 200 OK
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 2
date: Wed, 15 Mar 2023 12:27:56 GMT
server: envoy

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}
attempt 20:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    89  100    89    0     0  22250      0 --:--:-- --:--:-- --:--:-- 29666
HTTP/1.1 200 OK
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 2
date: Wed, 15 Mar 2023 12:27:58 GMT
server: envoy

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}

```

5. Open Kiali UI and authenticate using Openshift Credentials:
```shell
 xdg-open https://$(oc get route kiali -n istio-system -o=jsonpath="{.spec.host}")
```
