package com.dougkeen.bart.activities;

import android.content.ContentValues;
import android.view.View;
import android.widget.CheckBox;

import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.R;
import com.dougkeen.bart.data.RoutesColumns;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Station;

public class AddRouteDialogFragment extends AbstractRouteSelectionFragment {
	public AddRouteDialogFragment() {
		super(BartRunnerApplication.getAppContext().getString(
				R.string.add_route));
	}

	public AddRouteDialogFragment(String title) {
		super(title);
	}

	@Override
	public void onStart() {
		super.onStart();
		final View checkbox = getDialog().findViewById(R.id.return_checkbox);
		checkbox.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onOkButtonClick(Station origin, Station destination) {
		ContentValues values = new ContentValues();
		values.put(RoutesColumns.FROM_STATION.string, origin.abbreviation);
		values.put(RoutesColumns.TO_STATION.string, destination.abbreviation);

		getActivity().getContentResolver().insert(
				Constants.FAVORITE_CONTENT_URI, values);

		if (((CheckBox) getDialog().findViewById(R.id.return_checkbox))
				.isChecked()) {
			values = new ContentValues();
			values.put(RoutesColumns.FROM_STATION.string,
					destination.abbreviation);
			values.put(RoutesColumns.TO_STATION.string, origin.abbreviation);

			getActivity().getContentResolver().insert(
					Constants.FAVORITE_CONTENT_URI, values);
		}

		dismiss();
	}

}
