package com.dougkeen.bart.networktasks;

import com.dougkeen.bart.model.Constants;

import org.androidannotations.rest.spring.annotations.Get;
import org.androidannotations.rest.spring.annotations.Rest;

@Rest(rootUrl = "https://api.bart.gov", converters = {ElevatorMessageConverter.class})
public interface ElevatorClient {
    @Get("/api/bsa.aspx?cmd=elev&key=" + Constants.API_KEY)
    String getElevatorMessage();
}
