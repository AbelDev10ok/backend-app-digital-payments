package com.app.digital.payments.digital_pyments.configuration.security.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class SimpleGrentesAuthorityJson {
    
    @JsonCreator
    public SimpleGrentesAuthorityJson(@JsonProperty("authority") String role) {
        
    }
}
