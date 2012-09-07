package com.dougkeen.bart;

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
import com.dougkeen.bart.model.Departure;

public class TrainAlertDialogFragment extends DialogFragment {

	private static final String KEY_LAST_ALERT_DELAY = "lastAlertDelay";
	private Departure mDeparture;

	public TrainAlertDialogFragment(Departure mDeparture) {
		super();
		this.mDeparture = mDeparture;
	}

	@Override
	public void onStart() {
		super.onStart();

		SharedPreferences preferences = getActivity().getPreferences(
				Context.MODE_PRIVATE);
		int lastAlertDelay = preferences.getInt(KEY_LAST_ALERT_DELAY, 5);

		NumberPicker numberPicker = (NumberPicker) getDialog().findViewById(
				R.id.numberPicker);

		final int maxValue = mDeparture.getMeanSecondsLeft() / 60;

		String[] displayedValues = new String[maxValue];
		for (int i = 1; i <= maxValue; i++) {
			displayedValues[i - 1] = String.valueOf(i);
		}
		numberPicker.setMinValue(1);
		numberPicker.setMaxValue(maxValue);
		numberPicker.setDisplayedValues(displayedValues);

		if (maxValue >= lastAlertDelay) {
			numberPicker.setValue(lastAlertDelay);
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
				.setTitle(R.string.set_up_departure_alert)
				.setCancelable(true)
				.setView(R.layout.train_alert_dialog)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								NumberPicker numberPicker = (NumberPicker) getDialog()
										.findViewById(R.id.numberPicker);

								Editor editor = getActivity().getPreferences(
										Context.MODE_PRIVATE).edit();
								editor.putInt(KEY_LAST_ALERT_DELAY,
										numberPicker.getValue());
								editor.commit();
							}
						})
				.setNegativeButton(R.string.skip_alert,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dialog.cancel();
							}
						}).create();
	}
}
