package com.dougkeen.bart.activities;

import org.holoeverywhere.app.Activity;

import android.os.Bundle;
import android.webkit.WebView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.dougkeen.bart.R;

public class ViewMapActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		WebView webview = new WebView(this);
		setContentView(webview);

		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setSupportZoom(true);

		webview.loadUrl("file:///android_res/drawable/map.png");

		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.system_map_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
