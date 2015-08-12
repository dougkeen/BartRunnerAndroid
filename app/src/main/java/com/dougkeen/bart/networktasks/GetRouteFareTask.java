package com.dougkeen.bart.networktasks;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.xml.sax.SAXException;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Station;

public abstract class GetRouteFareTask extends
        AsyncTask<GetRouteFareTask.Params, Integer, String> {

    private final static int MAX_ATTEMPTS = 5;
    private final static String FARE_URL = "http://api.bart.gov/api/sched.aspx?cmd=fare&date=today&key="
            + Constants.API_KEY + "&orig=%1$s&dest=%2$s";

    private Exception mException;

    private String fare;

    @Override
    protected String doInBackground(Params... paramsArray) {
        Params params = paramsArray[0];

        if (!isCancelled()) {
            return getFareFromNetwork(params, 0);
        } else {
            return null;
        }
    }

    private String getFareFromNetwork(Params params, int attemptNumber) {
        String xml = null;

        try {
            HttpUriRequest request = new HttpGet(
                    String.format(FARE_URL, params.origin.abbreviation,
                            params.destination.abbreviation));

            FareContentHandler handler = new FareContentHandler();
            if (isCancelled()) {
                return null;
            }

            HttpResponse response = NetworkUtils.executeWithRecovery(request);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Server returned "
                        + response.getStatusLine().toString());
            }

            StringWriter writer = new StringWriter();
            IOUtils.copy(response.getEntity().getContent(), writer, "UTF-8");

            xml = writer.toString();
            if (xml.length() == 0) {
                throw new IOException("Server returned blank xml document");
            }

            Xml.parse(xml, handler);
            fare = handler.getFare();
            return fare;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            if (attemptNumber < MAX_ATTEMPTS - 1) {
                try {
                    Log.w(Constants.TAG,
                            "Attempt to contact server failed... retrying in 3s",
                            e);
                    Thread.sleep(3000);
                } catch (InterruptedException interrupt) {
                    // Ignore... just go on to next attempt
                }
                return getFareFromNetwork(params, attemptNumber + 1);
            } else {
                mException = new Exception("Could not contact BART system", e);
                return null;
            }
        } catch (SAXException e) {
            mException = new Exception(
                    "Could not understand response from BART system: " + xml, e);
            return null;
        }
    }

    public static class Params {
        public Params(Station origin, Station destination) {
            super();
            this.origin = origin;
            this.destination = destination;
        }

        public final Station origin;
        public final Station destination;
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            onResult(fare);
        } else {
            onError(mException);
        }
    }

    public abstract void onResult(String fare);

    public abstract void onError(Exception exception);

}