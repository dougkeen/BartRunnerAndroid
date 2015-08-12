package com.dougkeen.bart.networktasks;

import com.dougkeen.bart.model.Alert;
import com.dougkeen.bart.model.Constants;
import com.googlecode.androidannotations.annotations.rest.Get;
import com.googlecode.androidannotations.annotations.rest.Rest;

@Rest(rootUrl = "http://api.bart.gov", converters = {AlertListConverter.class})
public interface AlertsClient {
    @Get("/api/bsa.aspx?cmd=bsa&key=" + Constants.API_KEY)
    Alert.AlertList getAlerts();
}
