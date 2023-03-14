package com.redhat;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingServiceComposite implements GreetingService{
    @Override
    public GreetingBody generateGreeting(String version) {
         return GreetingBody
                 .builder()
                 .Greeting("Hello There")
                 .Type("General")
                 .from("The Greeting Application Version= " + version)
                 .build();
    }
}
