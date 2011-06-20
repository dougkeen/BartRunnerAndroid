package com.dougkeen.bart;

import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PowerManager;
import android.text.format.DateFormat;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.dougkeen.bart.GetRealTimeDeparturesTask.Params;
import com.dougkeen.bart.data.Departure;
import com.dougkeen.bart.data.RealTimeDepartures;
import com.dougkeen.bart.data.RoutesColumns;

public class ViewDeparturesActivity extends ListActivity {

	private static final int UNCERTAINTY_THRESHOLD = 17;

	private Uri mUri;

	private Station mOrigin;
	private Station mDestination;

	private ArrayAdapter<Departure> mDeparturesAdapter;

	private TextView mListTitleView;

	private AsyncTask<Params, Integer, RealTimeDepartures> mGetDeparturesTask;

	private boolean mIsAutoUpdating = false;

	private final Runnable AUTO_UPDATE_RUNNABLE = new Runnable() {
		@Override
		public void run() {
			runAutoUpdate();
		}
	};

	private PowerManager.WakeLock mWakeLock;

	private boolean mDataFetchIsPending;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Intent intent = getIntent();

		String action = intent.getAction();

		if (Intent.ACTION_VIEW.equals(action)) {
			mUri = intent.getData();
		}

		Cursor cursor = managedQuery(mUri, new String[] {
				RoutesColumns.FROM_STATION.string,
				RoutesColumns.TO_STATION.string }, null, null, null);

		if (!cursor.moveToFirst()) {
			throw new IllegalStateException("URI not found: " + mUri.toString());
		}
		mOrigin = Station.getByAbbreviation(cursor.getString(0));
		mDestination = Station.getByAbbreviation(cursor.getString(1));

		String header = "Departures:\n" + mOrigin.name + " to "
				+ mDestination.name;

		mListTitleView = (TextView) findViewById(R.id.listTitle);
		mListTitleView.setText(header);
		((TextView) findViewById(android.R.id.empty))
				.setText(R.string.departure_wait_message);

		mDeparturesAdapter = new DepartureArrayAdapter(this,
				R.layout.departure_listing);
		if (savedInstanceState != null
				&& savedInstanceState.containsKey("departures")) {
			for (Parcelable departure : savedInstanceState
					.getParcelableArray("departures")) {
				mDeparturesAdapter.add((Departure) departure);
			}
		}
		setListAdapter(mDeparturesAdapter);
	}

	@Override
	protected void onPause() {
		cancelDataFetch();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		cancelDataFetch();
		super.onDestroy();
	}

	private void cancelDataFetch() {
		if (mGetDeparturesTask != null) {
			mGetDeparturesTask.cancel(true);
			mDataFetchIsPending = false;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Departure[] departures = new Departure[mDeparturesAdapter.getCount()];
		for (int i = mDeparturesAdapter.getCount() - 1; i >= 0; i--) {
			departures[i] = mDeparturesAdapter.getItem(i);
		}
		outState.putParcelableArray("departures", departures);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			if (!mDataFetchIsPending) {
				fetchLatestDepartures();
			}
			PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = powerManager
					.newWakeLock(
							PowerManager.SCREEN_DIM_WAKE_LOCK,
							"ViewDeparturesActivity");
			mWakeLock.acquire();
			if (mDeparturesAdapter != null && !mDeparturesAdapter.isEmpty()) {
				mIsAutoUpdating = true;
			}
			runAutoUpdate();
		} else if (mWakeLock != null) {
			mWakeLock.release();
		}
	}

	private void fetchLatestDepartures() {
		if (!hasWindowFocus())
			return;
		if (mGetDeparturesTask != null
				&& mGetDeparturesTask.getStatus()
						.equals(AsyncTask.Status.RUNNING)) {
			// Don't overlap fetches
			return;
		}

		mGetDeparturesTask = new GetRealTimeDeparturesTask() {
			@Override
			public void onResult(RealTimeDepartures result) {
				mDataFetchIsPending = false;
				Log.i(Constants.TAG, "Processing data from server");
				processLatestDepartures(result);
				Log.i(Constants.TAG, "Done processing data from server");
			}

			@Override
			public void onError(Exception e) {
				mDataFetchIsPending = false;
				Log.w(Constants.TAG, e.getMessage(), e);
				Toast.makeText(ViewDeparturesActivity.this,
						R.string.could_not_connect,
						Toast.LENGTH_LONG).show();
				((TextView) findViewById(android.R.id.empty))
						.setText(R.string.could_not_connect);
			}
		};
		Log.i(Constants.TAG, "Fetching data from server");
		mGetDeparturesTask.execute(new GetRealTimeDeparturesTask.Params(
				mOrigin,
				mDestination));
	}

	protected void processLatestDepartures(RealTimeDepartures result) {
		if (result.getDepartures().isEmpty()) {
			final TextView textView = (TextView) findViewById(android.R.id.empty);
			textView.setText(R.string.no_data_message);
			Linkify.addLinks(textView, Linkify.WEB_URLS);
			return;
		}

		boolean needsBetterAccuracy = false;
		Departure firstDeparture = null;
		final List<Departure> departures = result.getDepartures();
		if (mDeparturesAdapter.getCount() > 0) {
			int adapterIndex = -1;
			for (Departure departure : departures) {
				adapterIndex++;
				Departure existingDeparture = null;
				if (adapterIndex < mDeparturesAdapter.getCount()) {
					existingDeparture = mDeparturesAdapter
							.getItem(adapterIndex);
				}
				while (existingDeparture != null
						&& !departure.equals(existingDeparture)) {
					mDeparturesAdapter.remove(existingDeparture);
					if (adapterIndex < mDeparturesAdapter.getCount()) {
						existingDeparture = mDeparturesAdapter
								.getItem(adapterIndex);
					} else {
						existingDeparture = null;
					}
				}
				if (existingDeparture != null) {
					existingDeparture.mergeEstimate(departure);
				} else {
					mDeparturesAdapter.add(departure);
					existingDeparture = departure;
				}
				if (firstDeparture == null) {
					firstDeparture = existingDeparture;
				}
				if (existingDeparture.getUncertaintySeconds() > UNCERTAINTY_THRESHOLD) {
					needsBetterAccuracy = true;
				}
			}
		} else {
			for (Departure departure : departures) {
				if (firstDeparture == null) {
					firstDeparture = departure;
				}
				mDeparturesAdapter.add(departure);
			}
			needsBetterAccuracy = true;
		}
		mDeparturesAdapter.notifyDataSetChanged();

		if (hasWindowFocus() && firstDeparture != null) {
			if (needsBetterAccuracy
					|| firstDeparture.hasDeparted()) {
				// Get more data in 20s
				scheduleDataFetch(20000);
			} else {
				// Get more 90 seconds before next train arrives, right when
				// next train arrives, or 3 minutes, whichever is sooner
				final int intervalUntilNextDeparture = firstDeparture
						.getMinSecondsLeft() * 1000;
				final int alternativeInterval = 3 * 60 * 1000;

				int interval = intervalUntilNextDeparture;
				if (intervalUntilNextDeparture > 95000
						&& intervalUntilNextDeparture < alternativeInterval) {
					interval = interval - 90 * 1000;
				} else if (intervalUntilNextDeparture > alternativeInterval) {
					interval = alternativeInterval;
				}

				if (interval < 0) {
					interval = 20000;
				}

				scheduleDataFetch(interval);
			}
			if (!mIsAutoUpdating) {
				mIsAutoUpdating = true;
			}
		} else {
			mIsAutoUpdating = false;
		}
	}

	private void scheduleDataFetch(int millisUntilExecute) {
		if (!mDataFetchIsPending) {
			mListTitleView.postDelayed(new Runnable() {
				@Override
				public void run() {
					fetchLatestDepartures();
				}
			}, millisUntilExecute);
			mDataFetchIsPending = true;
			Log.i(Constants.TAG, "Scheduled another data fetch in "
					+ millisUntilExecute / 1000 + "s");
		}
	}

	private void runAutoUpdate() {
		if (mIsAutoUpdating && mDeparturesAdapter != null) {
			mDeparturesAdapter.notifyDataSetChanged();
		}
		if (hasWindowFocus()) {
			mListTitleView.postDelayed(AUTO_UPDATE_RUNNABLE, 1000);
		} else {
			mIsAutoUpdating = false;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.route_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.view_on_bart_site_button) {
			startActivity(new Intent(
					Intent.ACTION_VIEW,
					Uri.parse("http://m.bart.gov/schedules/qp_results.aspx?type=departure&date=today&time="
							+ DateFormat.format("h:mmaa",
									System.currentTimeMillis()) + "&orig="
							+ mOrigin.abbreviation
							+ "&dest="
							+ mDestination.abbreviation)));
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}
