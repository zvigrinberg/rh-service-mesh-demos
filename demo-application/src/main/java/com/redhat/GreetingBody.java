package com.redhat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GreetingBody extends GreetingStructure{
    private String Greeting;
    private String Type;
    private String from;
//    private String to;
}
