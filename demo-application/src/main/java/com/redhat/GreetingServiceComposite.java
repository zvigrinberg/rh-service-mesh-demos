package com.redhat;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingServiceComposite implements GreetingService{
    private final long seed=786738252L;

    private String[] greetings ={"Have a nice Day!!",
                                 "Hello There!!",
                                 "Good Luck!!",
                                 "All the best!!",
                                 "Wish you prosperity and Wealth!",
                                 "Have a Shinny Day!",
                                 "Wish you Happy Resting!",
                                 "You're The Best!",
                                 "Best Regards!",
                                 "Wish you a Joyful Day"};
    @Override
    public GreetingBody generateGreeting(String version, String recipient) {
         return GreetingBody
                 .builder()
                 .Greeting(pickRandomGreeting())
                 .Type("General")
                 .from("The Greeting Application Version= " + version)
                 .to(recipient)
                 .build();
    }

    private String pickRandomGreeting()
    {
        int randomGreetingIndex = (int)(Math.random() * seed) % 10;
        return greetings[randomGreetingIndex];
    }
}
