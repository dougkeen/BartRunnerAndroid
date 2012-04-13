package com.dougkeen.bart.networktasks;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class NetworkUtils {

	public static HttpClient getHttpClient() {
		HttpClient client = new DefaultHttpClient();
		final HttpParams params = client.getParams();
		HttpConnectionParams.setConnectionTimeout(params,
				NetworkUtils.CONNECTION_TIMEOUT_MILLIS);
		HttpConnectionParams.setSoTimeout(params, NetworkUtils.CONNECTION_TIMEOUT_MILLIS);
		ConnManagerParams.setTimeout(params, NetworkUtils.CONNECTION_TIMEOUT_MILLIS);
		return client;
	}

	public static HttpResponse executeWithRecovery(final HttpUriRequest request)
			throws IOException, ClientProtocolException {
		try {
			return getHttpClient().execute(request);
		} catch (IllegalStateException e) {
			// try again... this is a rare error
			return getHttpClient().execute(request);
		}
	}

	static final int CONNECTION_TIMEOUT_MILLIS = 10000;

}
