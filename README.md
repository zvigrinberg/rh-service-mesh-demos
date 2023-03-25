# Red Hat service mesh Demos
Provide demos on features of  Redhat Service Mesh

## Prerequisites for demos:

All demos using demo-app , to deploy it, create a `namespace` "test" and deploy it using its manifests:
```shell
oc new-project test 
oc apply -f demo-application/openshift-manifests/v1/manifests.yaml

```

## Index
- [Installing RH Service Mesh Using the RH Service mesh Operator](./servicemesh-operator)
- List of Demos:
  - [Authentication And Authorization Using JWT (JSON Web Token)](./jwt-demo)
  - [Security Policies and mTLS Demo](./security-demo-and-mtls)
  - [Traffic Shifting/Splitting Demo](./traffic-management-demo/shifting-demo)
  - [Fault Injection Demo](./application-testing-resiliency-demo/fault-injection-demo)
  - [Mirroring Demo](./application-testing-resiliency-demo/mirroring-demo)

