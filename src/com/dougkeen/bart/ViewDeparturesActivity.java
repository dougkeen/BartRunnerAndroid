package com.dougkeen.bart;

import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PowerManager;
import android.text.format.DateFormat;
import android.text.util.Linkify;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dougkeen.bart.actionbarcompat.ActionBarListActivity;
import com.dougkeen.bart.controls.CountdownTextView;
import com.dougkeen.bart.controls.Ticker;
import com.dougkeen.bart.data.RoutesColumns;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.RealTimeDepartures;
import com.dougkeen.bart.model.ScheduleInformation;
import com.dougkeen.bart.model.ScheduleItem;
import com.dougkeen.bart.model.Station;
import com.dougkeen.bart.model.StationPair;
import com.dougkeen.bart.model.TextProvider;
import com.dougkeen.bart.networktasks.GetRealTimeDeparturesTask;
import com.dougkeen.bart.networktasks.GetScheduleInformationTask;

public class ViewDeparturesActivity extends ActionBarListActivity {

	private static final int UNCERTAINTY_THRESHOLD = 17;

	private Uri mUri;

	private Station mOrigin;
	private Station mDestination;
	private int mAverageTripLength;
	private int mAverageTripSampleCount;

	private Departure mSelectedDeparture;
	private Departure mBoardedDeparture;

	private DepartureArrayAdapter mDeparturesAdapter;

	private ScheduleInformation mLatestScheduleInfo;

	private TextView mEmptyView;

	private AsyncTask<StationPair, Integer, RealTimeDepartures> mGetDeparturesTask;
	private AsyncTask<StationPair, Integer, ScheduleInformation> mGetScheduleInformationTask;

	private PowerManager.WakeLock mWakeLock;

	private boolean mDepartureFetchIsPending;
	private boolean mScheduleFetchIsPending;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.departures);

		final Intent intent = getIntent();

		String action = intent.getAction();

		if (Intent.ACTION_VIEW.equals(action)) {
			mUri = intent.getData();
		}

		Cursor cursor = managedQuery(mUri, new String[] {
				RoutesColumns.FROM_STATION.string,
				RoutesColumns.TO_STATION.string,
				RoutesColumns.AVERAGE_TRIP_LENGTH.string,
				RoutesColumns.AVERAGE_TRIP_SAMPLE_COUNT.string }, null, null,
				null);

		if (!cursor.moveToFirst()) {
			throw new IllegalStateException("URI not found: " + mUri.toString());
		}
		mOrigin = Station.getByAbbreviation(cursor.getString(0));
		mDestination = Station.getByAbbreviation(cursor.getString(1));
		mAverageTripLength = cursor.getInt(2);
		mAverageTripSampleCount = cursor.getInt(3);

		((TextView) findViewById(R.id.listTitle)).setText(mOrigin.name + " to "
				+ mDestination.name);

		mEmptyView = (TextView) findViewById(android.R.id.empty);
		mEmptyView.setText(R.string.departure_wait_message);

		mDeparturesAdapter = new DepartureArrayAdapter(this,
				R.layout.departure_listing);
		if (savedInstanceState != null) {

			if (savedInstanceState.containsKey("departures")) {
				for (Parcelable departure : savedInstanceState
						.getParcelableArray("departures")) {
					mDeparturesAdapter.add((Departure) departure);
				}
			}
			if (savedInstanceState.containsKey("boardedDeparture")) {
				mBoardedDeparture = (Departure) savedInstanceState
						.getParcelable("boardedDeparture");
			}
		}
		setListAdapter(mDeparturesAdapter);

		registerForContextMenu(getListView());

		findViewById(R.id.missingDepartureText).setVisibility(View.VISIBLE);

		refreshBoardedDeparture();
	}

	@Override
	protected void onPause() {
		cancelDataFetch();
		Ticker.getInstance().stopTicking();
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
			mDepartureFetchIsPending = false;
		}
		if (mGetScheduleInformationTask != null) {
			mGetScheduleInformationTask.cancel(true);
			mScheduleFetchIsPending = false;
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
		outState.putParcelable("boardedDeparture", mBoardedDeparture);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Ticker.getInstance().startTicking();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			if (!mDepartureFetchIsPending) {
				fetchLatestDepartures();
			}
			PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = powerManager
					.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
							"ViewDeparturesActivity");
			mWakeLock.acquire();
			refreshBoardedDeparture();
		} else if (mWakeLock != null) {
			mWakeLock.release();
		}
	}

	private boolean mIgnoreDepartureDirection = false;

	private void fetchLatestDepartures() {
		if (!hasWindowFocus())
			return;
		if (mGetDeparturesTask != null
				&& mGetDeparturesTask.getStatus().equals(
						AsyncTask.Status.RUNNING)) {
			// Don't overlap fetches
			return;
		}

		mGetDeparturesTask = new GetRealTimeDeparturesTask(
				mIgnoreDepartureDirection) {
			@Override
			public void onResult(RealTimeDepartures result) {
				mDepartureFetchIsPending = false;
				Log.i(Constants.TAG, "Processing data from server");
				processLatestDepartures(result);
				Log.i(Constants.TAG, "Done processing data from server");
			}

			@Override
			public void onError(Exception e) {
				mDepartureFetchIsPending = false;
				Log.w(Constants.TAG, e.getMessage(), e);
				Toast.makeText(ViewDeparturesActivity.this,
						R.string.could_not_connect, Toast.LENGTH_LONG).show();
				mEmptyView.setText(R.string.could_not_connect);
				// Try again in 60s
				scheduleDepartureFetch(60000);
			}
		};
		Log.i(Constants.TAG, "Fetching data from server");
		mGetDeparturesTask.execute(new StationPair(mOrigin, mDestination));
	}

	private void fetchLatestSchedule() {
		if (!hasWindowFocus())
			return;
		if (mGetScheduleInformationTask != null
				&& mGetScheduleInformationTask.getStatus().equals(
						AsyncTask.Status.RUNNING)) {
			// Don't overlap fetches
			return;
		}

		mGetScheduleInformationTask = new GetScheduleInformationTask() {
			@Override
			public void onResult(ScheduleInformation result) {
				mScheduleFetchIsPending = false;
				Log.i(Constants.TAG, "Processing data from server");
				mLatestScheduleInfo = result;
				applyScheduleInformation();
				Log.i(Constants.TAG, "Done processing data from server");
			}

			@Override
			public void onError(Exception e) {
				mScheduleFetchIsPending = false;
				Log.w(Constants.TAG, e.getMessage(), e);
				Toast.makeText(ViewDeparturesActivity.this,
						R.string.could_not_connect, Toast.LENGTH_LONG).show();
				mEmptyView.setText(R.string.could_not_connect);
				// Try again in 60s
				scheduleScheduleInfoFetch(60000);
			}
		};
		Log.i(Constants.TAG, "Fetching data from server");
		mGetScheduleInformationTask.execute(new StationPair(mOrigin,
				mDestination));
	}

	protected void processLatestDepartures(RealTimeDepartures result) {
		if (result.getDepartures().isEmpty()) {
			result.includeTransferRoutes();
		}
		if (result.getDepartures().isEmpty()) {
			result.includeDoubleTransferRoutes();
		}
		if (result.getDepartures().isEmpty() && !mIgnoreDepartureDirection) {
			// Let's try again, ignoring direction (this sometimes comes up when
			// you travel between Millbrae and SFO)
			mIgnoreDepartureDirection = true;
			scheduleDepartureFetch(50);
			return;
		}
		if (result.getDepartures().isEmpty()) {
			final TextView textView = mEmptyView;
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
		requestScheduleIfNecessary();

		if (hasWindowFocus() && firstDeparture != null) {
			if (needsBetterAccuracy || firstDeparture.hasDeparted()) {
				// Get more data in 20s
				scheduleDepartureFetch(20000);
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

				scheduleDepartureFetch(interval);
			}
		}
	}

	private void requestScheduleIfNecessary() {
		if (mDeparturesAdapter.getCount() == 0) {
			return;
		}

		if (mLatestScheduleInfo == null) {
			fetchLatestSchedule();
			return;
		}

		Departure lastDeparture = mDeparturesAdapter.getItem(mDeparturesAdapter
				.getCount() - 1);
		if (mLatestScheduleInfo.getLatestDepartureTime() < lastDeparture
				.getMeanEstimate()) {
			fetchLatestSchedule();
			return;
		}
	}

	private void applyScheduleInformation() {
		int localAverageLength = mLatestScheduleInfo.getAverageTripLength();

		int departuresCount = mDeparturesAdapter.getCount();

		// Let's get smallest interval between departures
		int smallestDepartureInterval = 0;
		long previousDepartureTime = 0;
		for (int departureIndex = 0; departureIndex < departuresCount; departureIndex++) {
			Departure departure = mDeparturesAdapter.getItem(departureIndex);
			if (previousDepartureTime == 0) {
				previousDepartureTime = departure.getMeanEstimate();
			} else if (smallestDepartureInterval == 0) {
				smallestDepartureInterval = (int) (departure.getMeanEstimate() - previousDepartureTime);
			} else {
				smallestDepartureInterval = Math
						.min(smallestDepartureInterval,
								(int) (departure.getMeanEstimate() - previousDepartureTime));
			}
		}

		int lastSearchIndex = 0;
		int tripCount = mLatestScheduleInfo.getTrips().size();
		boolean departureUpdated = false;
		Departure lastUnestimatedTransfer = null;
		int departuresWithoutEstimates = 0;
		for (int departureIndex = 0; departureIndex < departuresCount; departureIndex++) {
			Departure departure = mDeparturesAdapter.getItem(departureIndex);
			for (int i = lastSearchIndex; i < tripCount; i++) {
				ScheduleItem trip = mLatestScheduleInfo.getTrips().get(i);
				if (!departure.getDestination().abbreviation.equals(trip
						.getTrainHeadStation())) {
					continue;
				}

				long departTimeDiff = Math.abs(trip.getDepartureTime()
						- departure.getMeanEstimate());
				final long millisUntilTripDeparture = trip.getDepartureTime()
						- System.currentTimeMillis();
				final int equalityTolerance = (departure.getOrigin() != null) ? NumberUtils
						.max(departure.getOrigin().departureEqualityTolerance,
								ScheduleItem.SCHEDULE_ITEM_DEPARTURE_EQUALS_TOLERANCE,
								smallestDepartureInterval)
						: ScheduleItem.SCHEDULE_ITEM_DEPARTURE_EQUALS_TOLERANCE;
				if (departure.getOrigin() != null
						&& departure.getOrigin().longStationLinger
						&& departure.hasDeparted()
						&& millisUntilTripDeparture > 0
						&& millisUntilTripDeparture < equalityTolerance) {
					departure.setArrivalTimeOverride(trip.getArrivalTime());
					lastSearchIndex = i;
					departureUpdated = true;
					if (lastUnestimatedTransfer != null) {
						lastUnestimatedTransfer.setArrivalTimeOverride(trip
								.getArrivalTime());
						departuresWithoutEstimates--;
					}
					break;
				} else if (departTimeDiff <= (equalityTolerance + departure
						.getUncertaintySeconds() * 1000)
						&& departure.getEstimatedTripTime() != trip
								.getTripLength()
						&& !(departure.getOrigin().longStationLinger && departure
								.hasDeparted())) {
					departure.setEstimatedTripTime(trip.getTripLength());
					lastSearchIndex = i;
					departureUpdated = true;
					if (lastUnestimatedTransfer != null) {
						lastUnestimatedTransfer.setArrivalTimeOverride(trip
								.getArrivalTime());
						departuresWithoutEstimates--;
					}
					break;
				}
			}
			// Don't estimate for non-scheduled transfers
			if (!departure.getRequiresTransfer()) {
				if (!departure.hasEstimatedTripTime() && localAverageLength > 0) {
					departure.setEstimatedTripTime(localAverageLength);
				} else if (!departure.hasEstimatedTripTime()) {
					departure.setEstimatedTripTime(mAverageTripLength);
				}
			} else if (departure.getRequiresTransfer()
					&& !departure.hasAnyArrivalEstimate()) {
				lastUnestimatedTransfer = departure;
			}
			if (!departure.hasAnyArrivalEstimate()) {
				departuresWithoutEstimates++;
			}
		}

		if (departureUpdated) {
			mDeparturesAdapter.notifyDataSetChanged();
		}

		// Update global average
		if (mLatestScheduleInfo.getTripCountForAverage() > 0) {
			int newAverageSampleCount = mAverageTripSampleCount
					+ mLatestScheduleInfo.getTripCountForAverage();
			int newAverage = (mAverageTripLength * mAverageTripSampleCount + localAverageLength
					* mLatestScheduleInfo.getTripCountForAverage())
					/ newAverageSampleCount;

			ContentValues contentValues = new ContentValues();
			contentValues.put(RoutesColumns.AVERAGE_TRIP_LENGTH.string,
					newAverage);
			contentValues.put(RoutesColumns.AVERAGE_TRIP_SAMPLE_COUNT.string,
					newAverageSampleCount);

			getContentResolver().update(mUri, contentValues, null, null);
		}

		if (departuresWithoutEstimates > 0) {
			scheduleScheduleInfoFetch(20000);
		}
	}

	private void scheduleDepartureFetch(int millisUntilExecute) {
		if (!mDepartureFetchIsPending) {
			postDelayed(new Runnable() {
				public void run() {
					fetchLatestDepartures();
				}
			}, millisUntilExecute);
			mDepartureFetchIsPending = true;
			Log.i(Constants.TAG, "Scheduled another departure fetch in "
					+ millisUntilExecute / 1000 + "s");
		}
	}

	private void scheduleScheduleInfoFetch(int millisUntilExecute) {
		if (!mScheduleFetchIsPending) {
			postDelayed(new Runnable() {
				public void run() {
					fetchLatestSchedule();
				}
			}, millisUntilExecute);
			mScheduleFetchIsPending = true;
			Log.i(Constants.TAG, "Scheduled another schedule fetch in "
					+ millisUntilExecute / 1000 + "s");
		}
	}

	private boolean postDelayed(Runnable runnable, long delayMillis) {
		return mEmptyView.postDelayed(runnable, delayMillis);
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
									System.currentTimeMillis())
							+ "&orig="
							+ mOrigin.abbreviation
							+ "&dest="
							+ mDestination.abbreviation)));
			return true;
		} else if (itemId == R.id.view_system_map_button) {
			startActivity(new Intent(this, ViewMapActivity.class));
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.departure_context_menu, menu);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		mSelectedDeparture = (Departure) getListAdapter()
				.getItem(info.position);
		menu.setHeaderTitle(R.string.departure_options);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.boardTrain) {
			mBoardedDeparture = mSelectedDeparture;
			refreshBoardedDeparture();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	private void refreshBoardedDeparture() {
		if (mBoardedDeparture == null)
			return;

		final Departure departure = mBoardedDeparture;
		findViewById(R.id.yourTrainSection).setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.yourTrainDestinationText))
				.setText(departure.getDestination().toString());

		((TextView) findViewById(R.id.yourTrainTrainLengthText))
				.setText(departure.getTrainLengthText());

		ImageView colorBar = (ImageView) findViewById(R.id.yourTrainDestinationColorBar);
		((GradientDrawable) colorBar.getDrawable()).setColor(Color
				.parseColor(departure.getDestinationColor()));
		if (departure.isBikeAllowed()) {
			((ImageView) findViewById(R.id.yourTrainBikeIcon))
					.setVisibility(View.VISIBLE);
		} else {
			((ImageView) findViewById(R.id.yourTrainBikeIcon))
					.setVisibility(View.INVISIBLE);
		}
		if (departure.getRequiresTransfer()) {
			((ImageView) findViewById(R.id.yourTrainXferIcon))
					.setVisibility(View.VISIBLE);
		} else {
			((ImageView) findViewById(R.id.yourTrainXferIcon))
					.setVisibility(View.INVISIBLE);
		}
		CountdownTextView departureCountdown = (CountdownTextView) findViewById(R.id.yourTrainDepartureCountdown);
		CountdownTextView arrivalCountdown = (CountdownTextView) findViewById(R.id.yourTrainArrivalCountdown);

		departureCountdown.setText("Leaves in " + departure.getCountdownText()
				+ " " + departure.getUncertaintyText());
		departureCountdown.setTextProvider(new TextProvider() {
			@Override
			public String getText() {
				if (departure.hasDeparted()) {
					return "Departed";
				} else {
					return "Leaves in " + departure.getCountdownText() + " "
							+ departure.getUncertaintyText();
				}
			}
		});

		arrivalCountdown
				.setText(departure.getEstimatedArrivalMinutesLeftText());
		arrivalCountdown.setTextProvider(new TextProvider() {
			@Override
			public String getText() {
				return departure.getEstimatedArrivalMinutesLeftText();
			}
		});
	}
}
