package com.dougkeen.bart;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class ViewMapActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		WebView webview = new WebView(this);
		setContentView(webview);

		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setSupportZoom(true);

		webview.loadUrl("file:///android_res/drawable/map.png");
	}
}
