package com.dougkeen.bart;

import com.dougkeen.bart.data.RoutesColumns;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

public class AddRouteActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.add_favorite);

		SpinnerAdapter originSpinnerAdapter = new ArrayAdapter<Station>(this,
				R.layout.simple_spinner_item, Station.values());
		((Spinner) findViewById(R.id.origin_spinner))
				.setAdapter(originSpinnerAdapter);

		SpinnerAdapter destinationSpinnerAdapter = new ArrayAdapter<Station>(
				this,
				R.layout.simple_spinner_item, Station.values());
		((Spinner) findViewById(R.id.destination_spinner))
				.setAdapter(destinationSpinnerAdapter);

		Button saveButton = (Button) findViewById(R.id.saveButton);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onSaveButtonClick();
			}

		});

		Button cancelButton = (Button) findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

	}

	protected void onSaveButtonClick() {
		Station origin = (Station) ((Spinner) findViewById(R.id.origin_spinner))
				.getSelectedItem();
		Station destination = (Station) ((Spinner) findViewById(R.id.destination_spinner))
				.getSelectedItem();

		if (origin == null) {
			Toast.makeText(this, com.dougkeen.bart.R.string.error_null_origin,
					Toast.LENGTH_LONG);
			return;
		}
		if (destination == null) {
			Toast.makeText(this,
					com.dougkeen.bart.R.string.error_null_destination,
					Toast.LENGTH_LONG);
			return;
		}
		if (origin.equals(destination)) {
			Toast.makeText(
					this,
					com.dougkeen.bart.R.string.error_matching_origin_and_destination,
					Toast.LENGTH_LONG);
			return;
		}

		ContentValues values = new ContentValues();
		values.put(RoutesColumns.FROM_STATION.string, origin.abbreviation);
		values.put(RoutesColumns.TO_STATION.string, destination.abbreviation);

		Uri newUri = getContentResolver().insert(
				Constants.FAVORITE_CONTENT_URI, values);
		setResult(RESULT_OK, (new Intent()).setAction(newUri.toString()));
		finish();
	}

}
