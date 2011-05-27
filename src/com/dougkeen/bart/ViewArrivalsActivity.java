package com.dougkeen.bart;

import java.io.IOException;
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
import android.util.Log;
import android.util.TimeFormatException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.dougkeen.bart.GetRealTimeArrivalsTask.Params;
import com.dougkeen.bart.data.Arrival;
import com.dougkeen.bart.data.RoutesColumns;
import com.dougkeen.bart.data.RealTimeArrivals;

public class ViewArrivalsActivity extends ListActivity {

	private static final String TAG = "BartCatcher";

	private static final int UNCERTAINTY_THRESHOLD = 17;

	private Uri mUri;

	private Station mOrigin;
	private Station mDestination;

	private ArrayAdapter<Arrival> mArrivalsAdapter;

	private TextView mListTitleView;

	private AsyncTask<Params, Integer, RealTimeArrivals> mGetArrivalsTask;

	private boolean mIsAutoUpdating = false;

	private final Runnable AUTO_UPDATE_RUNNABLE = new Runnable() {
		@Override
		public void run() {
			runAutoUpdate();
		}
	};

	private PowerManager.WakeLock mWakeLock;

	private boolean mFetchArrivalsOnNextFocus;

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

		String header = mOrigin.name + " to " + mDestination.name;

		mListTitleView = (TextView) findViewById(R.id.listTitle);
		mListTitleView.setText(header);
		((TextView) findViewById(android.R.id.empty))
				.setText(R.string.arrival_wait_message);

		mArrivalsAdapter = new ArrivalArrayAdapter(this,
				R.layout.arrival_listing);
		if (savedInstanceState != null
				&& savedInstanceState.containsKey("arrivals")) {
			for (Parcelable arrival : savedInstanceState
					.getParcelableArray("arrivals")) {
				mArrivalsAdapter.add((Arrival) arrival);
			}
		}
		setListAdapter(mArrivalsAdapter);

		mFetchArrivalsOnNextFocus = true;
	}

	@Override
	protected void onDestroy() {
		if (mGetArrivalsTask != null) {
			mGetArrivalsTask.cancel(true);
		}
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Arrival[] arrivals = new Arrival[mArrivalsAdapter.getCount()];
		for (int i = mArrivalsAdapter.getCount() - 1; i >= 0; i--) {
			arrivals[i] = mArrivalsAdapter.getItem(i);
		}
		outState.putParcelableArray("arrivals", arrivals);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			if (mFetchArrivalsOnNextFocus) {
				fetchLatestArrivals();
				mFetchArrivalsOnNextFocus = false;
			}
			PowerManager powerManaer = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = powerManaer.newWakeLock(
					PowerManager.SCREEN_DIM_WAKE_LOCK, "ViewArrivalsActivity");
			mWakeLock.acquire();
			if (mArrivalsAdapter != null && !mArrivalsAdapter.isEmpty()) {
				mIsAutoUpdating = true;
			}
			runAutoUpdate();
		} else if (mWakeLock != null) {
			mWakeLock.release();
		}
	}

	private void fetchLatestArrivals() {
		if (!hasWindowFocus())
			return;

		mGetArrivalsTask = new GetRealTimeArrivalsTask() {
			@Override
			public void onResult(RealTimeArrivals result) {
				Log.i(TAG, "Processing data from server");
				processLatestArrivals(result);
				Log.i(TAG, "Done processing data from server");
			}

			@Override
			public void onNetworkError(IOException e) {
				Toast.makeText(ViewArrivalsActivity.this, e.getMessage(),
						Toast.LENGTH_SHORT).show();
			}
		};
		Log.i(TAG, "Fetching data from server");
		mGetArrivalsTask.execute(new GetRealTimeArrivalsTask.Params(mOrigin,
				mDestination));
	}

	protected void processLatestArrivals(RealTimeArrivals result) {
		if (result.getArrivals().isEmpty()) {
			((TextView) findViewById(android.R.id.empty))
					.setText(R.string.no_data_message);
			return;
		}

		boolean needsBetterAccuracy = false;
		Arrival firstArrival = null;
		final List<Arrival> arrivals = result.getArrivals();
		if (mArrivalsAdapter.getCount() > 0) {
			int adapterIndex = -1;
			for (Arrival arrival : arrivals) {
				adapterIndex++;
				Arrival existingArrival = null;
				if (adapterIndex < mArrivalsAdapter.getCount()) {
					existingArrival = mArrivalsAdapter.getItem(adapterIndex);
				}
				while (existingArrival != null
						&& !arrival.equals(existingArrival)) {
					mArrivalsAdapter.remove(existingArrival);
					if (adapterIndex < mArrivalsAdapter.getCount()) {
						existingArrival = mArrivalsAdapter
								.getItem(adapterIndex);
					} else {
						existingArrival = null;
					}
				}
				if (existingArrival != null) {
					existingArrival.mergeEstimate(arrival);
				} else {
					mArrivalsAdapter.add(arrival);
					existingArrival = arrival;
				}
				if (firstArrival == null) {
					firstArrival = existingArrival;
				}
				if (existingArrival.getUncertaintySeconds() > UNCERTAINTY_THRESHOLD) {
					needsBetterAccuracy = true;
				}
			}
		} else {
			for (Arrival arrival : arrivals) {
				if (firstArrival == null) {
					firstArrival = arrival;
				}
				mArrivalsAdapter.add(arrival);
			}
			needsBetterAccuracy = true;
		}
		mArrivalsAdapter.notifyDataSetChanged();

		if (hasWindowFocus() && firstArrival != null) {
			if (needsBetterAccuracy
					|| firstArrival.hasArrived()) {
				// Get more data in 20s
				mListTitleView.postDelayed(new Runnable() {
					@Override
					public void run() {
						fetchLatestArrivals();
					}
				}, 20000);
				Log.i(TAG, "Scheduled another data fetch in 20s");
			} else {
				// Get more when next train arrives
				final int interval = firstArrival.getMinSecondsLeft() * 1000;
				mListTitleView.postDelayed(new Runnable() {
					@Override
					public void run() {
						fetchLatestArrivals();
					}
				}, interval);
				Log.i(TAG, "Scheduled another data fetch in " + interval / 1000
						+ "s");
			}
			if (!mIsAutoUpdating) {
				mIsAutoUpdating = true;
			}
		} else {
			mIsAutoUpdating = false;
		}
	}

	private void runAutoUpdate() {
		if (mIsAutoUpdating && mArrivalsAdapter != null) {
			mArrivalsAdapter.notifyDataSetChanged();
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
			mFetchArrivalsOnNextFocus = true;
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}
