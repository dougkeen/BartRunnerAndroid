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
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

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
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (args.containsKey("title"))
            mTitle = args.getString("title");
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

        ArrayAdapter<Station> originSpinnerAdapter = new ArrayAdapter<>(
                activity, R.layout.support_simple_spinner_dropdown_item,
                Station.getStationList());
        originSpinnerAdapter
                .setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        final Spinner originSpinner = (Spinner) dialog
                .findViewById(R.id.origin_spinner);
        originSpinner.setAdapter(originSpinnerAdapter);
        originSpinner.setSelection(lastSelectedOriginPosition);

        ArrayAdapter<Station> destinationSpinnerAdapter = new ArrayAdapter<>(
                activity, R.layout.support_simple_spinner_dropdown_item,
                Station.getStationList());
        destinationSpinnerAdapter
                .setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        final Spinner destinationSpinner = (Spinner) dialog
                .findViewById(R.id.destination_spinner);
        destinationSpinner.setAdapter(destinationSpinnerAdapter);
        destinationSpinner.setSelection(lastSelectedDestinationPosition);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        @SuppressLint("InflateParams")
        final View dialogView = LayoutInflater.from(activity)
                .inflate(R.layout.route_form, null /* root */);

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
        // TODO(fuegofro) - convert these toasts to error messages on the dialog.
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
