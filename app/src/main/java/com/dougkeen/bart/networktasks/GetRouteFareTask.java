package com.dougkeen.bart.networktasks;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Station;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

public abstract class GetRouteFareTask extends
        AsyncTask<GetRouteFareTask.Params, Integer, String> {

    private final static int MAX_ATTEMPTS = 5;
    private final static String FARE_URL = "http://api.bart.gov/api/sched.aspx?cmd=fare&date=today&key="
            + Constants.API_KEY + "&orig=%1$s&dest=%2$s";

    private final static OkHttpClient client = NetworkUtils.makeHttpClient();

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
            Request request = new Request.Builder()
                    .url(String.format(FARE_URL, params.origin.abbreviation, params.destination.abbreviation)).build();

            FareContentHandler handler = new FareContentHandler();
            if (isCancelled()) {
                return null;
            }

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new IOException("Server returned " + response.code());
            }

            xml = response.body().string();
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