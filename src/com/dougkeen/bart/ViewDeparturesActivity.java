package com.dougkeen.bart;

import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.dougkeen.bart.EtdService.EtdServiceBinder;
import com.dougkeen.bart.EtdService.EtdServiceListener;
import com.dougkeen.bart.controls.CountdownTextView;
import com.dougkeen.bart.controls.Ticker;
import com.dougkeen.bart.data.RoutesColumns;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.Station;
import com.dougkeen.bart.model.StationPair;
import com.dougkeen.bart.model.TextProvider;

public class ViewDeparturesActivity extends SherlockFragmentActivity implements
		EtdServiceListener {

	private static final int LOADER_ID = 123;

	private Uri mUri;

	private Station mOrigin;
	private Station mDestination;

	private Departure mSelectedDeparture;

	private DepartureArrayAdapter mDeparturesAdapter;

	private TextView mEmptyView;
	private ProgressBar mProgress;

	private ActionMode mActionMode;

	private EtdService mEtdService;

	private Handler mHandler = new Handler();

	private boolean mBound = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.departures);

		final Intent intent = getIntent();

		String action = intent.getAction();

		if (Intent.ACTION_VIEW.equals(action)) {
			mUri = intent.getData();
		}

		final Uri uri = mUri;

		if (savedInstanceState != null
				&& savedInstanceState.containsKey("origin")
				&& savedInstanceState.containsKey("destination")) {
			mOrigin = Station.getByAbbreviation(savedInstanceState
					.getString("origin"));
			mDestination = Station.getByAbbreviation(savedInstanceState
					.getString("destination"));
			setListTitle();
		} else {
			getSupportLoaderManager().initLoader(LOADER_ID, null,
					new LoaderCallbacks<Cursor>() {
						@Override
						public Loader<Cursor> onCreateLoader(int id, Bundle args) {
							return new CursorLoader(
									ViewDeparturesActivity.this, uri,
									new String[] {
											RoutesColumns.FROM_STATION.string,
											RoutesColumns.TO_STATION.string },
									null, null, null);
						}

						@Override
						public void onLoadFinished(Loader<Cursor> loader,
								Cursor cursor) {
							if (!cursor.moveToFirst()) {
								Log.wtf(Constants.TAG,
										"Couldn't find Route record for the current Activity");
							}
							mOrigin = Station.getByAbbreviation(cursor
									.getString(0));
							mDestination = Station.getByAbbreviation(cursor
									.getString(1));
							cursor.close();
							setListTitle();
							if (mBound && mEtdService != null)
								mEtdService
										.registerListener(ViewDeparturesActivity.this);
							refreshBoardedDeparture();

							getSupportLoaderManager().destroyLoader(LOADER_ID);
						}

						@Override
						public void onLoaderReset(Loader<Cursor> loader) {
							// ignore
						}
					});
		}

		mEmptyView = (TextView) findViewById(android.R.id.empty);
		mEmptyView.setText(R.string.departure_wait_message);

		mProgress = (ProgressBar) findViewById(android.R.id.progress);

		mDeparturesAdapter = new DepartureArrayAdapter(this,
				R.layout.departure_listing);

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("departures")) {
				for (Parcelable departure : savedInstanceState
						.getParcelableArray("departures")) {
					mDeparturesAdapter.add((Departure) departure);
				}
				mDeparturesAdapter.notifyDataSetChanged();
			}
			if (savedInstanceState.containsKey("selectedDeparture")) {
				mSelectedDeparture = (Departure) savedInstanceState
						.getParcelable("selectedDeparture");
			}
			if (savedInstanceState.getBoolean("hasActionMode")
					&& mSelectedDeparture != null) {
				startDepartureActionMode();
			}
		}
		setListAdapter(mDeparturesAdapter);
		getListView().setEmptyView(findViewById(android.R.id.empty));
		getListView().setOnItemClickListener(
				new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> adapterView,
							View view, int position, long id) {
						mSelectedDeparture = (Departure) getListAdapter()
								.getItem(position);
						if (mActionMode != null) {
							mActionMode.finish();
						}
						startDepartureActionMode();
					}
				});

		findViewById(R.id.missingDepartureText).setVisibility(View.VISIBLE);

		refreshBoardedDeparture();

		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if (((BartRunnerApplication) getApplication())
				.shouldPlayAlarmRingtone()) {
			soundTheAlarm();
		}
	}

	private MediaPlayer mMediaPlayer;

	private void soundTheAlarm() {
		Uri alertSound = RingtoneManager
				.getDefaultUri(RingtoneManager.TYPE_ALARM);

		if (alertSound == null || !tryToPlayRingtone(alertSound)) {
			alertSound = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			if (alertSound == null || !tryToPlayRingtone(alertSound)) {
				alertSound = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
			}
		}
		if (mMediaPlayer == null) {
			tryToPlayRingtone(alertSound);
		}
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				silenceAlarm();
			}
		}, 20000);

		((BartRunnerApplication) getApplication()).setPlayAlarmRingtone(false);

		Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.train_alert_text)
				.setCancelable(false)
				.setNeutralButton(R.string.silence_alarm,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								silenceAlarm();
								dialog.dismiss();
							}
						}).show();
	}

	private boolean tryToPlayRingtone(Uri alertSound) {
		mMediaPlayer = MediaPlayer.create(this, alertSound);
		if (mMediaPlayer == null)
			return false;
		mMediaPlayer.setLooping(true);
		mMediaPlayer.start();
		return true;
	}

	private void silenceAlarm() {
		try {
			if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
		} catch (IllegalStateException e) {
			Log.e(Constants.TAG,
					"Couldn't stop media player; It was in an invalid state", e);
		}
	}

	private void setListTitle() {
		((TextView) findViewById(R.id.listTitle)).setText(mOrigin.name + " to "
				+ mDestination.name);
	}

	@SuppressWarnings("unchecked")
	private AdapterView<ListAdapter> getListView() {
		return (AdapterView<ListAdapter>) findViewById(android.R.id.list);
	}

	private final ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mEtdService = null;
			mBound = false;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mEtdService = ((EtdServiceBinder) service).getService();
			mBound = true;
			if (getStationPair() != null) {
				mEtdService.registerListener(ViewDeparturesActivity.this);
			}
		}
	};

	protected DepartureArrayAdapter getListAdapter() {
		return mDeparturesAdapter;
	}

	protected void setListAdapter(DepartureArrayAdapter adapter) {
		mDeparturesAdapter = adapter;
		getListView().setAdapter(mDeparturesAdapter);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mEtdService != null)
			mEtdService.unregisterListener(this);
		if (mBound)
			unbindService(mConnection);
		Ticker.getInstance().stopTicking();
		WakeLocker.release();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mOrigin != null || mDestination != null) {
			/*
			 * If origin or destination are null, this thing was never
			 * initialized in the first place, so there's really nothing to save
			 */
			Departure[] departures = new Departure[mDeparturesAdapter
					.getCount()];
			for (int i = mDeparturesAdapter.getCount() - 1; i >= 0; i--) {
				departures[i] = mDeparturesAdapter.getItem(i);
			}
			outState.putParcelableArray("departures", departures);
			outState.putParcelable("selectedDeparture", mSelectedDeparture);
			outState.putBoolean("hasActionMode", mActionMode != null);
			outState.putString("origin", mOrigin.abbreviation);
			outState.putString("destination", mDestination.abbreviation);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		bindService(new Intent(this, EtdService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		Ticker.getInstance().startTicking();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			getWindow()
					.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			Ticker.getInstance().startTicking();
			refreshBoardedDeparture();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.route_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Constants.FAVORITE_CONTENT_URI);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		} else if (itemId == R.id.view_on_bart_site_button) {
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

	private void refreshBoardedDeparture() {
		final Departure boardedDeparture = ((BartRunnerApplication) getApplication())
				.getBoardedDeparture();
		if (boardedDeparture == null
				|| boardedDeparture.getStationPair() == null
				|| !boardedDeparture.getStationPair().equals(getStationPair())) {
			findViewById(R.id.yourTrainSection).setVisibility(View.GONE);
			return;
		}

		findViewById(R.id.yourTrainSection).setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.yourTrainDestinationText))
				.setText(boardedDeparture.getTrainDestination().toString());

		((TextView) findViewById(R.id.yourTrainTrainLengthText))
				.setText(boardedDeparture.getTrainLengthText());

		ImageView colorBar = (ImageView) findViewById(R.id.yourTrainDestinationColorBar);
		((GradientDrawable) colorBar.getDrawable()).setColor(Color
				.parseColor(boardedDeparture.getTrainDestinationColor()));
		if (boardedDeparture.isBikeAllowed()) {
			((ImageView) findViewById(R.id.yourTrainBikeIcon))
					.setVisibility(View.VISIBLE);
		} else {
			((ImageView) findViewById(R.id.yourTrainBikeIcon))
					.setVisibility(View.INVISIBLE);
		}
		if (boardedDeparture.getRequiresTransfer()) {
			((ImageView) findViewById(R.id.yourTrainXferIcon))
					.setVisibility(View.VISIBLE);
		} else {
			((ImageView) findViewById(R.id.yourTrainXferIcon))
					.setVisibility(View.INVISIBLE);
		}
		CountdownTextView departureCountdown = (CountdownTextView) findViewById(R.id.yourTrainDepartureCountdown);
		CountdownTextView arrivalCountdown = (CountdownTextView) findViewById(R.id.yourTrainArrivalCountdown);

		departureCountdown.setText("Leaves in "
				+ boardedDeparture.getCountdownText() + " "
				+ boardedDeparture.getUncertaintyText());
		departureCountdown.setTextProvider(new TextProvider() {
			@Override
			public String getText(long tickNumber) {
				if (boardedDeparture.hasDeparted()) {
					return "Departed";
				} else {
					return "Leaves in " + boardedDeparture.getCountdownText()
							+ " " + boardedDeparture.getUncertaintyText();
				}
			}
		});

		arrivalCountdown.setText(boardedDeparture
				.getEstimatedArrivalMinutesLeftText(this));
		arrivalCountdown.setTextProvider(new TextProvider() {
			@Override
			public String getText(long tickNumber) {
				return boardedDeparture
						.getEstimatedArrivalMinutesLeftText(ViewDeparturesActivity.this);
			}
		});
	}

	private void startDepartureActionMode() {
		mActionMode = startActionMode(new DepartureActionMode());
		mActionMode.setTitle(mSelectedDeparture.getTrainDestinationName());
		mActionMode.setSubtitle(mSelectedDeparture.getTrainLengthText());
	}

	private class DepartureActionMode implements ActionMode.Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.departure_context_menu, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (item.getItemId() == R.id.boardTrain) {
				final BartRunnerApplication application = (BartRunnerApplication) getApplication();
				mSelectedDeparture.setPassengerDestination(mDestination);
				application.setBoardedDeparture(mSelectedDeparture);
				refreshBoardedDeparture();

				// Stop the notification service
				/*stopService(new Intent(getApplicationContext(),
						NotificationService.class));*/

				// Don't prompt for alert if train is about to leave
				if (mSelectedDeparture.getMeanSecondsLeft() / 60 > 1) {
					new TrainAlertDialogFragment().show(
							getSupportFragmentManager(), "dialog");
				}

				mode.finish();
				return true;
			}
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
		}

	}

	@Override
	public void onETDChanged(final List<Departure> departures) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (departures.isEmpty()) {
					final TextView textView = mEmptyView;
					textView.setText(R.string.no_data_message);
					mProgress.setVisibility(View.INVISIBLE);
					Linkify.addLinks(textView, Linkify.WEB_URLS);
				} else {
					// Merge lists
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
								if (adapterIndex < mDeparturesAdapter
										.getCount()) {
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
						}
					} else {
						final DepartureArrayAdapter listAdapter = getListAdapter();
						listAdapter.clear();
						for (Departure departure : departures) {
							listAdapter.add(departure);
						}
					}

					refreshBoardedDeparture();

					getListAdapter().notifyDataSetChanged();
				}
			}
		});
	}

	@Override
	public void onError(final String errorMessage) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(ViewDeparturesActivity.this, errorMessage,
						Toast.LENGTH_LONG).show();
			}
		});
	}

	@Override
	public void onRequestStarted() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mProgress.setVisibility(View.VISIBLE);
			}
		});
	}

	@Override
	public void onRequestEnded() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mProgress.setVisibility(View.INVISIBLE);
			}
		});
	}

	@Override
	public StationPair getStationPair() {
		if (mOrigin == null || mDestination == null)
			return null;
		return new StationPair(mOrigin, mDestination);
	}
}
