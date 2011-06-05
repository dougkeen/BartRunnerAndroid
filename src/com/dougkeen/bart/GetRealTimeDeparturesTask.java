package com.dougkeen.bart;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.xml.sax.SAXException;

import com.dougkeen.bart.data.RealTimeDepartures;

import android.os.AsyncTask;
import android.util.Xml;

public abstract class GetRealTimeDeparturesTask extends
		AsyncTask<GetRealTimeDeparturesTask.Params, Integer, RealTimeDepartures> {

	private static final int CONNECTION_TIMEOUT_MILLIS = 10000;
	private final static String API_KEY = "5LD9-IAYI-TRAT-MHHW";
	private final static String API_URL = "http://api.bart.gov/api/etd.aspx?cmd=etd&key="
								+ API_KEY + "&orig=%1$s&dir=%2$s";
	private final static int MAX_ATTEMPTS = 3;

	private IOException mIOException;

	private List<Route> mRoutes;

	@Override
	protected RealTimeDepartures doInBackground(Params... paramsArray) {
		// Always expect one param
		Params params = paramsArray[0];

		mRoutes = params.origin.getRoutesForDestination(params.destination);

		if (!isCancelled()) {
			return getDeparturesFromNetwork(params, 0);
		} else {
			return null;
		}
	}

	private RealTimeDepartures getDeparturesFromNetwork(Params params,
			int attemptNumber) {
		try {
			URL sourceUrl = new URL(String.format(API_URL,
					params.origin.abbreviation, mRoutes.get(0).getDirection()));

			EtdContentHandler handler = new EtdContentHandler(params.origin,
					params.destination, mRoutes);
			if (isCancelled()) {
				return null;
			}
			URLConnection connection = sourceUrl.openConnection();
			connection.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
			Xml.parse(connection.getInputStream(),
					Xml.findEncodingByName("UTF-8"),
					handler);
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
					Thread.sleep(5000);
				} catch (InterruptedException interrupt) {
					// Ignore... just go on to next attempt
				}
				return getDeparturesFromNetwork(params, attemptNumber + 1);
			} else {
				mIOException = e;
				return null;
			}
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
	}

	public static class Params {
		public Params(Station origin, Station destination) {
			super();
			this.origin = origin;
			this.destination = destination;
		}

		private Station origin;
		private Station destination;

		public Station getOrigin() {
			return origin;
		}

		public Station getDestination() {
			return destination;
		}
	}

	@Override
	protected void onPostExecute(RealTimeDepartures result) {
		if (result != null) {
			onResult(result);
		} else {
			onNetworkError(mIOException);
		}
	}

	public abstract void onResult(RealTimeDepartures result);

	public abstract void onNetworkError(IOException e);
}
