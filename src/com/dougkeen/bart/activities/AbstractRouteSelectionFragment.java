package com.dougkeen.bart.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.WazaBe.HoloEverywhere.ArrayAdapter;
import com.WazaBe.HoloEverywhere.LayoutInflater;
import com.WazaBe.HoloEverywhere.app.AlertDialog;
import com.WazaBe.HoloEverywhere.app.Dialog;
import com.WazaBe.HoloEverywhere.app.DialogFragment;
import com.WazaBe.HoloEverywhere.widget.Spinner;
import com.WazaBe.HoloEverywhere.widget.Toast;
import com.dougkeen.bart.R;
import com.dougkeen.bart.model.Station;

public abstract class AbstractRouteSelectionFragment extends DialogFragment {

	private static final String KEY_LAST_SELECTED_DESTINATION = "lastSelectedDestination";
	private static final String KEY_LAST_SELECTED_ORIGIN = "lastSelectedOrigin";
	protected String mTitle;

	public AbstractRouteSelectionFragment(String title) {
		super();
		mTitle = title;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setShowsDialog(true);
	}

	@Override
	public void onStart() {
		super.onStart();

		SharedPreferences preferences = getActivity().getPreferences(
				Context.MODE_PRIVATE);

		final int lastSelectedOriginPosition = preferences.getInt(
				KEY_LAST_SELECTED_ORIGIN, 0);
		final int lastSelectedDestinationPosition = preferences.getInt(
				KEY_LAST_SELECTED_DESTINATION, 1);

		final Dialog dialog = getDialog();
		final FragmentActivity activity = getActivity();
		ArrayAdapter<Station> originSpinnerAdapter = new ArrayAdapter<Station>(
				activity, R.layout.simple_spinner_item,
				Station.getStationList());
		originSpinnerAdapter
				.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

		final Spinner originSpinner = (Spinner) dialog
				.findViewById(R.id.origin_spinner);
		originSpinner.setAdapter(originSpinnerAdapter);
		originSpinner.setSelection(lastSelectedOriginPosition);

		ArrayAdapter<Station> destinationSpinnerAdapter = new ArrayAdapter<Station>(
				activity, R.layout.simple_spinner_item,
				Station.getStationList());
		destinationSpinnerAdapter
				.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

		final Spinner destinationSpinner = (Spinner) dialog
				.findViewById(R.id.destination_spinner);
		destinationSpinner.setAdapter(destinationSpinnerAdapter);
		destinationSpinner.setSelection(lastSelectedDestinationPosition);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final FragmentActivity activity = getActivity();

		final View dialogView = LayoutInflater.inflate(activity,
				R.layout.route_form);

		return new AlertDialog.Builder(activity)
				.setTitle(mTitle)
				.setCancelable(true)
				.setView(dialogView)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								handleOkClick();
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dialog.cancel();
							}
						}).create();
	}

	protected void handleOkClick() {
		final Dialog dialog = getDialog();
		final Spinner originSpinner = (Spinner) dialog
				.findViewById(R.id.origin_spinner);
		final Spinner destinationSpinner = (Spinner) dialog
				.findViewById(R.id.destination_spinner);

		Station origin = (Station) originSpinner.getSelectedItem();
		Station destination = (Station) destinationSpinner.getSelectedItem();
		if (origin == null) {
			Toast.makeText(dialog.getContext(),
					com.dougkeen.bart.R.string.error_null_origin,
					Toast.LENGTH_LONG).show();
			return;
		}
		if (destination == null) {
			Toast.makeText(dialog.getContext(),
					com.dougkeen.bart.R.string.error_null_destination,
					Toast.LENGTH_LONG).show();
			return;
		}
		if (origin.equals(destination)) {
			Toast.makeText(
					dialog.getContext(),
					com.dougkeen.bart.R.string.error_matching_origin_and_destination,
					Toast.LENGTH_LONG).show();
			return;
		}

		final Editor prefsEditor = getActivity().getPreferences(
				Context.MODE_PRIVATE).edit();
		prefsEditor.putInt(KEY_LAST_SELECTED_ORIGIN,
				originSpinner.getSelectedItemPosition());
		prefsEditor.putInt(KEY_LAST_SELECTED_DESTINATION,
				destinationSpinner.getSelectedItemPosition());
		prefsEditor.commit();

		onOkButtonClick(origin, destination);
	}

	abstract protected void onOkButtonClick(Station origin, Station destination);
}