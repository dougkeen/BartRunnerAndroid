package com.dougkeen.bart.activities;

import android.view.View;
import android.widget.CheckBox;

import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.R;
import com.dougkeen.bart.model.Station;
import com.dougkeen.bart.model.StationPair;

public class AddRouteDialogFragment extends AbstractRouteSelectionFragment {
    public static final String TAG = "ADD_ROUTE_DIALOG_FRAGMENT_TAG";

    public AddRouteDialogFragment() {
        super(BartRunnerApplication.getAppContext().getString(
                R.string.add_route));
    }

    @Override
    public void onStart() {
        super.onStart();
        final View checkbox = getDialog().findViewById(R.id.return_checkbox);
        checkbox.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onOkButtonClick(Station origin, Station destination) {
        RoutesListActivity activity = (RoutesListActivity) getActivity();
        activity.addFavorite(new StationPair(origin, destination));

        if (((CheckBox) getDialog().findViewById(R.id.return_checkbox))
                .isChecked()) {
            activity.addFavorite(new StationPair(destination, origin));
        }

        dismiss();
    }

}
