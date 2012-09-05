package com.dougkeen.bart;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.WazaBe.HoloEverywhere.AlertDialog;
import com.dougkeen.bart.model.Station;

public abstract class AbstractRouteSelectionFragment extends DialogFragment {

	protected String mTitle;

	public AbstractRouteSelectionFragment(String title) {
		super();
		mTitle = title;
	}

	@Override
	public void onStart() {
		super.onStart();

		final Dialog dialog = getDialog();
		final FragmentActivity activity = getActivity();
		ArrayAdapter<Station> originSpinnerAdapter = new ArrayAdapter<Station>(
				activity, android.R.layout.simple_spinner_item,
				Station.getStationList());
		originSpinnerAdapter
				.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
		((Spinner) dialog.findViewById(R.id.origin_spinner))
				.setAdapter(originSpinnerAdapter);

		ArrayAdapter<Station> destinationSpinnerAdapter = new ArrayAdapter<Station>(
				activity, android.R.layout.simple_spinner_item,
				Station.getStationList());
		destinationSpinnerAdapter
				.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

		((Spinner) dialog.findViewById(R.id.destination_spinner))
				.setAdapter(destinationSpinnerAdapter);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final FragmentActivity activity = getActivity();

		return new AlertDialog.Builder(activity)
				.setTitle(mTitle)
				.setCancelable(true)
				.setView(R.layout.route_form)
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
		Station origin = (Station) ((Spinner) dialog
				.findViewById(R.id.origin_spinner)).getSelectedItem();
		Station destination = (Station) ((Spinner) dialog
				.findViewById(R.id.destination_spinner)).getSelectedItem();
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
		onOkButtonClick(origin, destination);
	}

	abstract protected void onOkButtonClick(Station origin, Station destination);
}