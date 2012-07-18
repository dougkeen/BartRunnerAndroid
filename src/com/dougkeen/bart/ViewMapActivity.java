package com.dougkeen.bart;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;

import com.dougkeen.bart.actionbarcompat.ActionBarActivity;
import com.dougkeen.bart.model.Constants;

public class ViewMapActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		WebView webview = new WebView(this);
		setContentView(webview);

		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setSupportZoom(true);

		webview.loadUrl("file:///android_res/drawable/map.png");

		getActionBarHelper().setHomeButtonEnabled(true);
		getActionBarHelper().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.system_map_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			startActivity(new Intent(Intent.ACTION_PICK,
					Constants.FAVORITE_CONTENT_URI));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
