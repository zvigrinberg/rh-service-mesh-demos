# Mirroring Demo

## Goal And Motivation

In case you want to test in production new features that are not changing anything , for instance , GET endpoints ( not changing resources/entities, only fetching data).
Then you can route the request to the older version, but in the same time to mirror the request to another service, and discarding its response.
This powerful feature doesn't affect clients on production environment( they still get the response of the old version) , and you can still check the log of the newer version pod for the request that various clients invoked.

### Demo Procedure:

1. Delete any former policy resource
```shell
 oc delete virtualservices.networking.istio.io,destinationrule -l policyResource="true"
```

2. Create a Destination rule, which selecting to each subset the pods of the corresponding version, based on pods labels:
```shell
oc apply -f destination-rule-subseting.yaml
```
3. Create the `VirtualService` That will route all traffic to version v1, but will mirror all of it in parallel to v2 as well.
```shell
oc apply -f virtual-service-traffic-mirroring.yaml
```

4. Run 10 times the service
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
5. As you can see, all traffic routed to v1, and we got appropriate responses, but Check logs of v2 to see that all traffic mirrored to it as well:
```shell
oc logs -l app=demo-app,version=v2 --tail=150
```
Output:
```shell
Starting the Java application using /opt/jboss/container/java/run/run-java.sh ...
INFO exec  java -Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager -XX:+UseParallelGC -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:+ExitOnOutOfMemoryError -cp "." -jar /deployments/quarkus-run.jar 
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
10:39:36 INFO  traceId=, parentId=, spanId=, sampled= [io.qu.sm.op.ru.OpenApiRecorder] (main) CORS filtering is disabled and cross-origin resource sharing is allowed without restriction, which is not recommended in production. Please configure the CORS filter through 'quarkus.http.cors.*' properties. For more information, see Quarkus HTTP CORS documentation
 10:39:36 INFO  traceId=, parentId=, spanId=, sampled= [io.quarkus] (main) demo-app 1.0.0-SNAPSHOT on JVM (powered by Quarkus 2.16.4.Final) started in 0.892s. Listening on: http://0.0.0.0:8080
 10:39:36 INFO  traceId=, parentId=, spanId=, sampled= [io.quarkus] (main) Profile prod activated. 
 10:39:36 INFO  traceId=, parentId=, spanId=, sampled= [io.quarkus] (main) Installed features: [cdi, hibernate-validator, jaeger, resteasy, resteasy-jackson, smallrye-context-propagation, smallrye-openapi, smallrye-opentracing, vertx]
 10:44:35 INFO  traceId=803c38ef534754d3, parentId=0, spanId=803c38ef534754d3, sampled=false [co.re.GreetingResource] (executor-thread-0) Received request, App Version=v2, About to return next response Body : 
 {
  "from" : "The Greeting Application Version= v2",
  "to" : "John Doe",
  "type" : "General",
  "greeting" : "Have a Shinny Day!"
}
 10:44:37 INFO  traceId=0a14c45290719250, parentId=0, spanId=a14c45290719250, sampled=false [co.re.GreetingResource] (executor-thread-0) Received request, App Version=v2, About to return next response Body : 
 {
  "from" : "The Greeting Application Version= v2",
  "to" : "John Doe",
  "type" : "General",
  "greeting" : "Have a nice Day!!"
}
 10:44:39 INFO  traceId=447059561db7d22b, parentId=0, spanId=447059561db7d22b, sampled=false [co.re.GreetingResource] (executor-thread-0) Received request, App Version=v2, About to return next response Body : 
 {
  "from" : "The Greeting Application Version= v2",
  "to" : "John Doe",
  "type" : "General",
  "greeting" : "All the best!!"
}
 10:44:41 INFO  traceId=408841ac4d0eca92, parentId=0, spanId=408841ac4d0eca92, sampled=false [co.re.GreetingResource] (executor-thread-0) Received request, App Version=v2, About to return next response Body : 
 {
  "from" : "The Greeting Application Version= v2",
  "to" : "John Doe",
  "type" : "General",
  "greeting" : "Hello There!!"
}
 10:44:42 INFO  traceId=c0a5ff1fd910908e, parentId=0, spanId=c0a5ff1fd910908e, sampled=false [co.re.GreetingResource] (executor-thread-0) Received request, App Version=v2, About to return next response Body : 
 {
  "from" : "The Greeting Application Version= v2",
  "to" : "John Doe",
  "type" : "General",
  "greeting" : "Wish you prosperity and Wealth!"
}
 10:44:44 INFO  traceId=fecbbfae9a8eb675, parentId=0, spanId=fecbbfae9a8eb675, sampled=false [co.re.GreetingResource] (executor-thread-0) Received request, App Version=v2, About to return next response Body : 
 {
  "from" : "The Greeting Application Version= v2",
  "to" : "John Doe",
  "type" : "General",
  "greeting" : "Have a Shinny Day!"
}
 10:44:46 INFO  traceId=bd8378bcfe6971de, parentId=0, spanId=bd8378bcfe6971de, sampled=false [co.re.GreetingResource] (executor-thread-0) Received request, App Version=v2, About to return next response Body : 
 {
  "from" : "The Greeting Application Version= v2",
  "to" : "John Doe",
  "type" : "General",
  "greeting" : "Wish you Happy Resting!"
}
 10:44:48 INFO  traceId=44c2c0d98541c1e8, parentId=0, spanId=44c2c0d98541c1e8, sampled=false [co.re.GreetingResource] (executor-thread-0) Received request, App Version=v2, About to return next response Body : 
 {
  "from" : "The Greeting Application Version= v2",
  "to" : "John Doe",
  "type" : "General",
  "greeting" : "Have a nice Day!!"
}
 10:44:49 INFO  traceId=7decd299fc531dc5, parentId=0, spanId=7decd299fc531dc5, sampled=false [co.re.GreetingResource] (executor-thread-0) Received request, App Version=v2, About to return next response Body : 
 {
  "from" : "The Greeting Application Version= v2",
  "to" : "John Doe",
  "type" : "General",
  "greeting" : "Have a nice Day!!"
}
 10:44:51 INFO  traceId=09196e4aa68f8bc8, parentId=0, spanId=9196e4aa68f8bc8, sampled=false [co.re.GreetingResource] (executor-thread-0) Received request, App Version=v2, About to return next response Body : 
 {
  "from" : "The Greeting Application Version= v2",
  "to" : "John Doe",
  "type" : "General",
  "greeting" : "Wish you a Joyful Day"
}
```
Note: This way you can test the features implemented in v2 in production without affecting clients. 