package com.dougkeen.bart;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.webkit.WebView;

import com.dougkeen.bart.actionbarcompat.ActionBarActivity;

public class ViewMapActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		WebView webview = new WebView(this);
		setContentView(webview);

		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setSupportZoom(true);

		webview.loadUrl("file:///android_res/drawable/map.png");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.system_map_menu, menu);
		return true;
	}

}
