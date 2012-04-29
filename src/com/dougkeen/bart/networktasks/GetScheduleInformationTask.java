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
import com.dougkeen.bart.model.ScheduleInformation;
import com.dougkeen.bart.model.StationPair;

public abstract class GetScheduleInformationTask extends
		AsyncTask<StationPair, Integer, ScheduleInformation> {

	private final static String SCHED_URL = "http://api.bart.gov/api/sched.aspx?cmd=depart&key="
			+ Constants.API_KEY + "&orig=%1$s&dest=%2$s&b=1&a=4";

	private final static int MAX_ATTEMPTS = 5;

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

			HttpUriRequest request = new HttpGet(url);

			if (isCancelled()) {
				return null;
			}

			ScheduleContentHandler handler = new ScheduleContentHandler(
					params.getOrigin(), params.getDestination());

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
