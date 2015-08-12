package com.dougkeen.bart.networktasks;

import com.dougkeen.bart.model.Constants;
import com.googlecode.androidannotations.annotations.rest.Get;
import com.googlecode.androidannotations.annotations.rest.Rest;

@Rest(rootUrl = "http://api.bart.gov", converters = {ElevatorMessageConverter.class})
public interface ElevatorClient {
    @Get("/api/bsa.aspx?cmd=elev&key=" + Constants.API_KEY)
    String getElevatorMessage();
}
