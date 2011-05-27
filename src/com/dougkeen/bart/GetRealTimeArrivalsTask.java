package com.dougkeen.bart;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.xml.sax.SAXException;

import com.dougkeen.bart.data.RealTimeArrivals;

import android.os.AsyncTask;
import android.util.Xml;

public abstract class GetRealTimeArrivalsTask extends
		AsyncTask<GetRealTimeArrivalsTask.Params, Integer, RealTimeArrivals> {

	private final static String API_KEY = "5LD9-IAYI-TRAT-MHHW";
	private final static String API_URL = "http://api.bart.gov/api/etd.aspx?cmd=etd&key="
								+ API_KEY + "&orig=%1$s&dir=%2$s";
	private final static int MAX_ATTEMPTS = 3;

	private IOException mIOException;

	private List<Route> mRoutes;

	@Override
	protected RealTimeArrivals doInBackground(Params... paramsArray) {
		// Always expect one param
		Params params = paramsArray[0];

		mRoutes = params.origin.getRoutesForDestination(params.destination);

		URL sourceUrl;
		try {
			sourceUrl = new URL(String.format(API_URL,
					params.origin.abbreviation, mRoutes.get(0).getDirection()));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		if (!isCancelled()) {
			return getArrivalsFromNetwork(params, sourceUrl, 0);
		} else {
			return null;
		}
	}

	private RealTimeArrivals getArrivalsFromNetwork(Params params,
			URL sourceUrl, int attemptNumber) {
		try {
			EtdContentHandler handler = new EtdContentHandler(params.origin,
					params.destination, mRoutes);
			if (isCancelled()) {
				return null;
			}
			Xml.parse(sourceUrl.openStream(), Xml.findEncodingByName("UTF-8"),
					handler);
			final RealTimeArrivals realTimeArrivals = handler
					.getRealTimeArrivals();
			return realTimeArrivals;
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
				return getArrivalsFromNetwork(params, sourceUrl,
						attemptNumber++);
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
	protected void onPostExecute(RealTimeArrivals result) {
		if (result != null) {
			onResult(result);
		} else {
			onNetworkError(mIOException);
		}
	}

	public abstract void onResult(RealTimeArrivals result);

	public abstract void onNetworkError(IOException e);
}
