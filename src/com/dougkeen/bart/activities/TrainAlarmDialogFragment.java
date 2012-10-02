package com.dougkeen.bart.activities;

import net.simonvt.widget.NumberPicker;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import com.WazaBe.HoloEverywhere.AlertDialog;
import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.R;
import com.dougkeen.bart.model.Departure;

public class TrainAlarmDialogFragment extends DialogFragment {

	private static final String KEY_LAST_ALARM_LEAD_TIME = "lastAlarmLeadTime";

	public TrainAlarmDialogFragment() {
		super();
	}

	@Override
	public void onStart() {
		super.onStart();

		SharedPreferences preferences = getActivity().getPreferences(
				Context.MODE_PRIVATE);
		int lastAlarmLeadTime = preferences.getInt(KEY_LAST_ALARM_LEAD_TIME, 5);

		NumberPicker numberPicker = (NumberPicker) getDialog().findViewById(
				R.id.numberPicker);

		BartRunnerApplication application = (BartRunnerApplication) getActivity()
				.getApplication();

		final Departure boardedDeparture = application.getBoardedDeparture();
		final int maxValue = boardedDeparture.getMeanSecondsLeft() / 60;

		String[] displayedValues = new String[maxValue];
		for (int i = 1; i <= maxValue; i++) {
			displayedValues[i - 1] = String.valueOf(i);
		}
		numberPicker.setMinValue(1);
		numberPicker.setMaxValue(maxValue);
		numberPicker.setDisplayedValues(displayedValues);

		if (boardedDeparture.isAlarmPending()) {
			numberPicker.setValue(boardedDeparture.getAlarmLeadTimeMinutes());
		} else if (maxValue >= lastAlarmLeadTime) {
			numberPicker.setValue(lastAlarmLeadTime);
		} else if (maxValue >= 5) {
			numberPicker.setValue(5);
		} else if (maxValue >= 3) {
			numberPicker.setValue(3);
		} else {
			numberPicker.setValue(1);
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final FragmentActivity activity = getActivity();

		return new AlertDialog.Builder(activity)
				.setTitle(R.string.set_up_departure_alarm)
				.setCancelable(true)
				.setView(R.layout.train_alarm_dialog)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								NumberPicker numberPicker = (NumberPicker) getDialog()
										.findViewById(R.id.numberPicker);
								final int alarmLeadTime = numberPicker
										.getValue();

								// Save most recent selection
								Editor editor = getActivity().getPreferences(
										Context.MODE_PRIVATE).edit();
								editor.putInt(KEY_LAST_ALARM_LEAD_TIME,
										alarmLeadTime);
								editor.commit();

								((BartRunnerApplication) getActivity()
										.getApplication())
										.getBoardedDeparture().setUpAlarm(
												alarmLeadTime);
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
}
