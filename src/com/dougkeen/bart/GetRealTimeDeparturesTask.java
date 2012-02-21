package com.dougkeen.bart;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.xml.sax.SAXException;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import com.dougkeen.bart.data.RealTimeDepartures;

public abstract class GetRealTimeDeparturesTask
		extends
		AsyncTask<GetRealTimeDeparturesTask.Params, Integer, RealTimeDepartures> {

	private final static String ETD_URL = "http://api.bart.gov/api/etd.aspx?cmd=etd&key="
			+ Constants.API_KEY + "&orig=%1$s&dir=%2$s";
	private final static String ETD_URL_NO_DIRECTION = "http://api.bart.gov/api/etd.aspx?cmd=etd&key="
			+ Constants.API_KEY + "&orig=%1$s";
	private final static int MAX_ATTEMPTS = 5;

	private Exception mException;

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
		String xml = null;
		try {
			String url;
			if (params.origin.endOfLine) {
				url = String.format(ETD_URL_NO_DIRECTION,
						params.origin.abbreviation);
			} else {
				url = String.format(ETD_URL, params.origin.abbreviation,
						mRoutes.get(0).getDirection());
			}

			HttpUriRequest request = new HttpGet(url);

			EtdContentHandler handler = new EtdContentHandler(params.origin,
					params.destination, mRoutes);
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
			onError(mException);
		}
	}

	public abstract void onResult(RealTimeDepartures result);

	public abstract void onError(Exception exception);
}
