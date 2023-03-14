package com.redhat;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;


@Path("/hello")
public class GreetingResource {

private static final Logger LOG = Logger.getLogger(GreetingResource.class);
private static final ObjectMapper om = new ObjectMapper();
    @ConfigProperty(name = "environment.app.version")
    private String app_version;

    @Inject
    private GreetingService greetingService;
    @SneakyThrows
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GreetingStructure hello(@DefaultValue ("John Doe") @QueryParam("recipient") String recipient) {
//        String app_version = System.getenv("APP_VERSION");

        GreetingStructure greetingMessage = greetingService.generateGreeting(app_version,recipient);
        String jsonResponse = om.writerWithDefaultPrettyPrinter().writeValueAsString(greetingMessage);
        LOG.infof("Received request, App Version=" + app_version + ", About to return next response Body : \n %s", jsonResponse);
        return greetingMessage;
    }
}