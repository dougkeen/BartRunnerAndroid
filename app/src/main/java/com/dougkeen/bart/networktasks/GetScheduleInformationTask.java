package com.dougkeen.bart.networktasks;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.ScheduleInformation;
import com.dougkeen.bart.model.StationPair;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

public abstract class GetScheduleInformationTask extends
        AsyncTask<StationPair, Integer, ScheduleInformation> {

    private final static String SCHED_URL = "http://api.bart.gov/api/sched.aspx?cmd=depart&key="
            + Constants.API_KEY + "&orig=%1$s&dest=%2$s&b=1&a=4";

    private final static int MAX_ATTEMPTS = 5;

    private final static OkHttpClient client = NetworkUtils.makeHttpClient();

    private Exception mException;

    @Override
    protected ScheduleInformation doInBackground(StationPair... paramsArray) {
        // Always expect one param
        StationPair params = paramsArray[0];

        if (!isCancelled()) {
            return getScheduleFromNetwork(params, 0);
        } else {
            return null;
        }
    }

    private ScheduleInformation getScheduleFromNetwork(StationPair params,
                                                       int attemptNumber) {
        String xml = null;
        try {
            String url = String.format(SCHED_URL,
                    params.getOrigin().abbreviation,
                    params.getDestination().abbreviation);

            Request request = new Request.Builder().url(url).build();

            if (isCancelled()) {
                return null;
            }

            ScheduleContentHandler handler = new ScheduleContentHandler(
                    params.getOrigin(), params.getDestination());

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new IOException("Server returned " + response.code());
            }

            xml = response.body().string();
            if (xml.length() == 0) {
                throw new IOException("Server returned blank xml document");
            }

            Xml.parse(xml, handler);
            final ScheduleInformation schedule = handler.getSchedule();
            return schedule;
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
                return getScheduleFromNetwork(params, attemptNumber + 1);
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

    @Override
    protected void onPostExecute(ScheduleInformation result) {
        if (result != null) {
            onResult(result);
        } else {
            onError(mException);
        }
    }

    public abstract void onResult(ScheduleInformation result);

    public abstract void onError(Exception exception);
}
