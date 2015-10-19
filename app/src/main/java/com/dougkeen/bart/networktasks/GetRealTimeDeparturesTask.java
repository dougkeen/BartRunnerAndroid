package com.dougkeen.bart.networktasks;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.List;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.RealTimeDepartures;
import com.dougkeen.bart.model.Route;
import com.dougkeen.bart.model.StationPair;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public abstract class GetRealTimeDeparturesTask extends
        AsyncTask<StationPair, Integer, RealTimeDepartures> {

    private final static String ETD_URL = "http://api.bart.gov/api/etd.aspx?cmd=etd&key="
            + Constants.API_KEY + "&orig=%1$s&dir=%2$s";
    private final static String ETD_URL_NO_DIRECTION = "http://api.bart.gov/api/etd.aspx?cmd=etd&key="
            + Constants.API_KEY + "&orig=%1$s";
    private final static int MAX_ATTEMPTS = 5;

    private final OkHttpClient mClient = NetworkUtils.makeHttpClient();

    private Exception mException;

    private List<Route> mRoutes;

    private final boolean ignoreDirection;

    public GetRealTimeDeparturesTask(boolean ignoreDirection) {
        super();
        this.ignoreDirection = ignoreDirection;
    }

    @Override
    protected RealTimeDepartures doInBackground(StationPair... paramsArray) {
        // Always expect one param
        StationPair params = paramsArray[0];

        mRoutes = params.getOrigin().getDirectRoutesForDestination(
                params.getDestination());

        boolean hasDirectLine = false;
        for (Route route : mRoutes) {
            if (!route.hasTransfer()) {
                hasDirectLine = true;
                break;
            }
        }

        if (mRoutes.isEmpty()
                || (params.getOrigin().transferFriendly && !hasDirectLine)) {
            mRoutes.addAll(params.getOrigin().getTransferRoutes(
                    params.getDestination()));
        }

        if (!isCancelled()) {
            return getDeparturesFromNetwork(params, 0);
        } else {
            return null;
        }
    }

    private RealTimeDepartures getDeparturesFromNetwork(StationPair params,
                                                        int attemptNumber) {
        String xml = null;
        try {
            String url;
            if (ignoreDirection || params.getOrigin().endOfLine) {
                url = String.format(ETD_URL_NO_DIRECTION,
                        params.getOrigin().abbreviation);
            } else {
                url = String.format(ETD_URL, params.getOrigin().abbreviation,
                        mRoutes.get(0).getDirection());
            }


            Request request = new Request.Builder()
                    .url(url).build();

            EtdContentHandler handler = new EtdContentHandler(
                    params.getOrigin(), params.getDestination(), mRoutes);
            if (isCancelled()) {
                return null;
            }

            Response response = mClient.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new IOException("Server returned " + response.code());
            }

            xml = response.body().string();
            if (xml.length() == 0) {
                throw new IOException("Server returned blank xml document");
            }

            try {
                Xml.parse(xml, handler);
            } catch (Exception e) {
                mException = new IOException("Server returned malformed xml: "
                        + xml);
                return null;
            }
            final RealTimeDepartures realTimeDepartures = handler
                    .getRealTimeDepartures();
            return realTimeDepartures;
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
                return getDeparturesFromNetwork(params, attemptNumber + 1);
            } else {
                mException = new Exception("Could not contact BART system", e);
                return null;
            }
        }
    }

    @Override
    protected void onPostExecute(RealTimeDepartures result) {
        if (result != null) {
            onResult(result);
        } else {
            onError(mException);
        }
    }

    public abstract void onResult(RealTimeDepartures result);

    public abstract void onError(Exception exception);
}
