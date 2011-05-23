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
import android.os.PowerManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.dougkeen.bart.GetRealTimeArrivalsTask.Params;
import com.dougkeen.bart.data.Arrival;
import com.dougkeen.bart.data.FavoritesColumns;
import com.dougkeen.bart.data.RealTimeArrivals;

public class ViewArrivalsActivity extends ListActivity {

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Intent intent = getIntent();

		String action = intent.getAction();

		if (Intent.ACTION_VIEW.equals(action)) {
			mUri = intent.getData();
		}

		Cursor cursor = managedQuery(mUri, new String[] {
				FavoritesColumns.FROM_STATION.string,
				FavoritesColumns.TO_STATION.string }, null, null, null);

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

		mArrivalsAdapter = new ArrayAdapter<Arrival>(
				this, R.layout.simple_spinner_item);
		setListAdapter(mArrivalsAdapter);

		fetchLatestArrivals();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			PowerManager powerManaer = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = powerManaer.newWakeLock(
					PowerManager.SCREEN_DIM_WAKE_LOCK, "ViewArrivalsActivity");
			mWakeLock.acquire();
		} else if (mWakeLock != null) {
			mWakeLock.release();
		}
	}

	private void fetchLatestArrivals() {
		mGetArrivalsTask = new GetRealTimeArrivalsTask() {
			@Override
			public void onResult(RealTimeArrivals result) {
				processLatestArrivals(result);
			}

			@Override
			public void onNetworkError(IOException e) {
				Toast.makeText(ViewArrivalsActivity.this, e.getMessage(),
						Toast.LENGTH_SHORT).show();
			}
		};
		mGetArrivalsTask.execute(new GetRealTimeArrivalsTask.Params(mOrigin,
				mDestination));
	}

	protected void processLatestArrivals(RealTimeArrivals result) {
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
			}
		} else {
			for (Arrival arrival : arrivals) {
				if (firstArrival == null) {
					firstArrival = arrival;
				}
				mArrivalsAdapter.add(arrival);
			}
		}
		mArrivalsAdapter.notifyDataSetChanged();

		if (hasWindowFocus() && firstArrival != null) {
			if (firstArrival.getUncertaintySeconds() > 17
					|| firstArrival.getMinutes() == 0) {
				// Get more data in 20s
				mListTitleView.postDelayed(new Runnable() {
					@Override
					public void run() {
						fetchLatestArrivals();
					}
				}, 20000);
			} else {
				// Get more when next train arrives
				mListTitleView.postDelayed(new Runnable() {
					@Override
					public void run() {
						fetchLatestArrivals();
					}
				}, firstArrival.getMinSecondsLeft() * 1000);
			}
			if (!mIsAutoUpdating) {
				mIsAutoUpdating = true;
				runAutoUpdate();
			}
		} else {
			mIsAutoUpdating = false;
		}

	}

	private void runAutoUpdate() {
		if (mIsAutoUpdating) {
			mArrivalsAdapter.notifyDataSetChanged();
		}
		if (hasWindowFocus()) {
			mListTitleView.postDelayed(AUTO_UPDATE_RUNNABLE, 1000);
		} else {
			mIsAutoUpdating = false;
		}
	}
}
