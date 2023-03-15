# JWT (Json Web-Token) Authentication Service Mesh Demo

## Prerequisite

- We'll use a RSA256 Signed JWT that was prepared in advance for this demo
```json

//Header Part
{
  "alg": "RS256",
  "kid": "rsaKey",
  "typ": "JWT"
}
```
```json
//Payload (Claims) Part
{
  "cloud-native": "true",
  "exp": 4685989700,
  "iat": 1674389501,
  "iss": "zgrinber@redhat.com",
  "sub": "zgrinber@redhat.com"
}
```


## Procedure

1. Define `RequestAuthentication` Object to configure a JWT authentication to a workload using JWT Rule - the JWT issuer must be
   as defined in the rule, and the JWT' signature will be verified against the JWKS ( Json Web Key Set) which is located in the specified Uri.
```yaml
apiVersion: security.istio.io/v1beta1
kind: RequestAuthentication
metadata:
  name: "jwt-auth"
  namespace: demo
  labels:
    demoMeshResource: "true"
    policyResource: "true"
spec:
  selector:
    matchLabels:
      app: demo-app
  jwtRules:
    - issuer: "zgrinber@redhat.com"
      jwksUri: "https://raw.githubusercontent.com/zvigrinberg/Cloud-native-patterns/main/isolated-secrets-store-k8s/jwt/jkws-demo.json"


```
```shell
oc apply -f resources/request-authentication.yaml
```
2. The former guarantees that any attempt of authentication using JWT will fail if will not
   pass the signature verification and JWT issuer conditions, but if the request will not be authenticated, then it will still
   be permitted. In order to restrict access to only authenticated requests, let's define an `AuthorizationPolicy`
```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: require-jwt
  namespace: demo
  labels:
    demoMeshResource: "true"
    policyResource: "true"
spec:
  selector:
    matchLabels:
      app: demo-app
  action: ALLOW
  rules:
    - from:
        - source:
            requestPrincipals: ["zgrinber@redhat.com/zgrinber@redhat.com"]
```
```shell
oc apply -f resources/authorization-policy-allow-principal.yaml
```

3. Create a rest client pod without envoy proxy sidecar.
```shell
oc run rest-api-client -it --image=ubi8/ubi:8.5-226 -n demo --command -- bash
```

4. Authenticate using a valid JWT token:
```shell
 export TOKEN=$(cat ./jwt/demo.jwt) ; oc exec rest-api-client -n demo sh -- curl http://demo-app-v1:8080/hello --header 'Authorization: Bearer '$TOKEN'' ; echo
```
Output:
```shell
HTTP/1.1 200 OK
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 2
date: Tue, 14 Mar 2023 14:33:03 GMT
server: istio-envoy
x-envoy-decorator-operation: demo-app-v1.demo.svc.cluster.local:8080/*

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}
```
5. Now manipulate the signature part of the JWT, and try to authenticate using invalid JWT value:
```shell
INVALID_TOKEN=$(echo $TOKEN | awk -F . '{print $1"."$2".abcdefghijklmnopqrstuvwxyz"}' | cat) ; oc exec rest-api-client -n demo sh -- curl -i http://demo-app-v1:8080/hello --header 'Authorization: Bearer '$INVALID_TOKEN'' ; echo
```
Output:
```shell
HTTP/1.1 401 Unauthorized
www-authenticate: Bearer realm="http://demo-app-v1:8080/hello", error="invalid_token"
content-length: 22
content-type: text/plain
date: Tue, 14 Mar 2023 14:35:21 GMT
server: istio-envoy
x-envoy-decorator-operation: demo-app-v1.demo.svc.cluster.local:8080/*
```
6. Now Try to invoke the get method of the service without token at all:
```shell
 oc exec rest-api-client -n demo sh -- curl -i http://demo-app-v1:8080/hello --header 'Authorization: Bearer ' 
```
Output:
```shell
-HTTP/1.1 403 Forbidden  0
content-length: 19
content-type: text/plain
date: Tue, 14 Mar 2023 14:37:30 GMT
server: istio-envoy
x-envoy-decorator-operation: demo-app-v1.demo.svc.cluster.local:8080/*

RBAC: access denied
```

7. Delete Authorization Policy to apply more examples:
```shell
oc delete -f resources/authorization-policy-allow-principal.yaml
```

8. Apply Authorization Policy to match negation of an arbitrary raw JWT claim in a JWT
```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  labels:
    demoMeshResource: "true"
    policyResource: "true"
  name: require-jwt-negation-raw-cloud-native-claim
  namespace: demo
spec:
  selector:
    matchLabels:
      app: demo-app
  action: ALLOW
  rules:
    - from:
        - source:
            requestPrincipals: ["zgrinber@redhat.com/zgrinber@redhat.com"]
      to:
      - operation:
          methods: ["GET"]
      when:
      - key: request.auth.claims[cloud-native]
        notValues: ["true"]
```
```shell
oc apply -f resources/authorization-policy-allow-get-when-not-cloud-native.yaml
```
9. Try now to access the service using the same JWT.
```shell
export TOKEN=$(cat ./jwt/demo.jwt) ; oc exec rest-api-client -n demo sh -- curl -i  http://demo-app-v1:8080/hello --header 'Authorization: Bearer '$TOKEN'' ; echo
```
Output:
```shell
HTTP/1.1 403 Forbidden
content-length: 19
content-type: text/plain
date: Tue, 14 Mar 2023 14:49:07 GMT
server: istio-envoy
x-envoy-decorator-operation: demo-app-v1.demo.svc.cluster.local:8080/*

RBAC: access denied
```

10. The Reason that the request was unauthorized, is that the JWT claims part contains a raw claim cloud-native: true,
    Let's delete the last `AuthorizationPolicy`, And will create a new `AuthorizationPolicy` that will match the claim cloud-native: true
```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  labels:
    demoMeshResource: "true"
    policyResource: "true"
  name: require-jwt-raw-cloud-native-claim
  namespace: demo
spec:
  selector:
    matchLabels:
      app: demo-app
  action: ALLOW
  rules:
    - from:
        - source:
            requestPrincipals: ["zgrinber@redhat.com/zgrinber@redhat.com"]
      to:
      - operation:
          methods: ["GET"]
      when:
      - key: request.auth.claims[cloud-native]
        values: ["true"]
```
```shell
oc delete -f resources/authorization-policy-allow-get-when-not-cloud-native.yaml
oc apply -f resources/authorization-policy-allow-get-when-cloud-native.yaml
```

11. Try now again to access the service using the same JWT , you'll see it'll succeed this time:
```shell
export TOKEN=$(cat ./jwt/demo.jwt) ; oc exec rest-api-client -n demo sh -- curl -i  http://demo-app-v1:8080/hello --header 'Authorization: Bearer '$TOKEN'' ; echo
```
Output:
```shell
HTTP/1.1 200 OK
content-type: application/json
content-length: 89
x-envoy-upstream-service-time: 3
date: Tue, 14 Mar 2023 14:59:56 GMT
server: istio-envoy
x-envoy-decorator-operation: demo-app-v1.demo.svc.cluster.local:8080/*

{"from":"The Greeting Application Version= v1","type":"General","greeting":"Hello There"}
```

12. Clean all Policies manifests files of this demo and client pod
```shell
oc delete authorizationpolicy,requestauthentication -l policyResource=true
oc delete pod rest-api-client
```
