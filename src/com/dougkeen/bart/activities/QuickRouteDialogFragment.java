package com.dougkeen.bart.activities;

import android.content.Intent;

import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.R;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Station;

public class QuickRouteDialogFragment extends AbstractRouteSelectionFragment {

	public QuickRouteDialogFragment() {
		super(BartRunnerApplication.getAppContext().getString(
				R.string.quick_departure_lookup));
	}

	@Override
	protected void onOkButtonClick(Station origin, Station destination) {
		startActivity(new Intent(Intent.ACTION_VIEW,
				Constants.ARBITRARY_ROUTE_CONTENT_URI_ROOT.buildUpon()
						.appendPath(origin.abbreviation)
						.appendPath(destination.abbreviation).build()));
	}
}
