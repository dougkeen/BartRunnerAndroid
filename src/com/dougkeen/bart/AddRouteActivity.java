package com.dougkeen.bart;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import com.dougkeen.bart.data.RoutesColumns;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Station;

public class AddRouteActivity extends AbstractRouteSelectionActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final View checkboxText = findViewById(R.id.return_checkbox_text);
		final View checkbox = findViewById(R.id.return_checkbox);
		checkboxText.setVisibility(View.VISIBLE);
		checkbox.setVisibility(View.VISIBLE);
		checkboxText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				checkbox.performClick();
			}
		});
	}

	@Override
	protected void onOkButtonClick(Station origin, Station destination) {
		ContentValues values = new ContentValues();
		values.put(RoutesColumns.FROM_STATION.string, origin.abbreviation);
		values.put(RoutesColumns.TO_STATION.string, destination.abbreviation);

		Uri newUri = getContentResolver().insert(
				Constants.FAVORITE_CONTENT_URI, values);

		if (((CheckBox) findViewById(R.id.return_checkbox)).isChecked()) {
			values = new ContentValues();
			values.put(RoutesColumns.FROM_STATION.string,
					destination.abbreviation);
			values.put(RoutesColumns.TO_STATION.string, origin.abbreviation);

			getContentResolver().insert(Constants.FAVORITE_CONTENT_URI, values);
		}

		setResult(RESULT_OK, (new Intent()).setAction(newUri.toString()));
		finish();
	}
}
