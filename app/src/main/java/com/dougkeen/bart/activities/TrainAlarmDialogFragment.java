package com.dougkeen.bart.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.R;
import com.dougkeen.bart.model.Departure;

public class TrainAlarmDialogFragment extends DialogFragment {

    public static final String TAG = "TRAIN_ALARM_DIALOG_FRAGMENT_TAG";
    private static final String KEY_LAST_ALARM_LEAD_TIME = "lastAlarmLeadTime";

    public TrainAlarmDialogFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowsDialog(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        setUpNumberPickerValues(getDialog());
    }

    private void setUpNumberPickerValues(Dialog dialog) {
        SharedPreferences preferences = getActivity().getPreferences(
                Context.MODE_PRIVATE);
        int lastAlarmLeadTime = preferences.getInt(KEY_LAST_ALARM_LEAD_TIME, 5);

        NumberPicker numberPicker = (NumberPicker) dialog
                .findViewById(R.id.numberPicker);

        BartRunnerApplication application = (BartRunnerApplication) getActivity()
                .getApplication();

        final Departure boardedDeparture = application.getBoardedDeparture();
        final int maxValue = boardedDeparture.getMeanSecondsLeft() / 60;

        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(maxValue);

        if (boardedDeparture.isAlarmPending()) {
            setNumber(numberPicker, boardedDeparture.getAlarmLeadTimeMinutes());
        } else if (maxValue >= lastAlarmLeadTime) {
            setNumber(numberPicker, lastAlarmLeadTime);
        } else if (maxValue >= 5) {
            setNumber(numberPicker, 5);
        } else if (maxValue >= 3) {
            setNumber(numberPicker, 3);
        } else {
            setNumber(numberPicker, 1);
        }
    }

    private void setNumber(NumberPicker numberPicker, int value) {
        numberPicker.setValue(value);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        @SuppressLint("InflateParams")
        final View dialogView = LayoutInflater.from(activity)
                .inflate(R.layout.train_alarm_dialog, null /* root */);

        return new AlertDialog.Builder(activity)
                .setTitle(R.string.set_up_departure_alarm)
                .setCancelable(true)
                .setView(dialogView)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                NumberPicker numberPicker = (NumberPicker) getDialog()
                                        .findViewById(R.id.numberPicker);
                                final int alarmLeadTime = numberPicker.getValue();

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
