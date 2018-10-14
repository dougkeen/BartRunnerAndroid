package com.dougkeen.bart.networktasks;

import com.dougkeen.bart.model.Alert;
import com.dougkeen.bart.model.Constants;

import org.androidannotations.rest.spring.annotations.Get;
import org.androidannotations.rest.spring.annotations.Rest;

@Rest(rootUrl = "https://api.bart.gov", converters = {AlertListConverter.class})
public interface AlertsClient {
    @Get("/api/bsa.aspx?cmd=bsa&key=" + Constants.API_KEY)
    Alert.AlertList getAlerts();
}
