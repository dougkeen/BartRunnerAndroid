package com.dougkeen.bart.activities;

import java.util.Calendar;
import java.util.TimeZone;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;

import com.WazaBe.HoloEverywhere.app.AlertDialog;
import com.WazaBe.HoloEverywhere.app.AlertDialog.Builder;
import com.WazaBe.HoloEverywhere.app.DialogFragment;
import com.WazaBe.HoloEverywhere.sherlock.SActivity;
import com.WazaBe.HoloEverywhere.widget.ListView;
import com.WazaBe.HoloEverywhere.widget.TextView;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.dougkeen.bart.R;
import com.dougkeen.bart.controls.Ticker;
import com.dougkeen.bart.controls.Ticker.TickSubscriber;
import com.dougkeen.bart.data.CursorUtils;
import com.dougkeen.bart.data.FavoritesArrayAdapter;
import com.dougkeen.bart.data.RoutesColumns;
import com.dougkeen.bart.model.Alert;
import com.dougkeen.bart.model.Alert.AlertList;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Station;
import com.dougkeen.bart.model.StationPair;
import com.dougkeen.bart.networktasks.AlertsClient;
import com.dougkeen.bart.networktasks.ElevatorClient;
import com.dougkeen.bart.networktasks.GetRouteFareTask;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ItemClick;
import com.googlecode.androidannotations.annotations.ItemLongClick;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.googlecode.androidannotations.annotations.rest.RestService;

@EActivity(R.layout.main)
public class RoutesListActivity extends SActivity implements
		LoaderCallbacks<Cursor>, TickSubscriber {
	private static final String NO_DELAYS_REPORTED = "No delays reported";

	private static final int FAVORITES_LOADER_ID = 0;

	private static final TimeZone PACIFIC_TIME = TimeZone
			.getTimeZone("America/Los_Angeles");

	private Uri mCurrentlySelectedUri;

	private Station mCurrentlySelectedOrigin;
	private Station mCurrentlySelectedDestination;

	private ActionMode mActionMode;

	private FavoritesArrayAdapter mRoutesAdapter;

	@RestService
	AlertsClient alertsClient;

	@RestService
	ElevatorClient elevatorClient;

	@ViewById(android.R.id.list)
	ListView listView;

	@ViewById(R.id.quickLookupButton)
	Button quickLookupButton;

	@ViewById(R.id.alertMessages)
	TextView alertMessages;

	@Click(R.id.quickLookupButton)
	void quickLookupButtonClick() {
		DialogFragment dialog = new QuickRouteDialogFragment(
				getString(R.string.quick_departure_lookup));
		dialog.show(getSupportFragmentManager().beginTransaction());
	}

	@ItemClick(android.R.id.list)
	void listItemClicked(StationPair item) {
		startActivity(new Intent(Intent.ACTION_VIEW,
				ContentUris.withAppendedId(Constants.FAVORITE_CONTENT_URI,
						item.getId())));
	}

	@ItemLongClick(android.R.id.list)
	void listItemLongClick(StationPair item) {
		if (mActionMode != null) {
			mActionMode.finish();
		}

		mCurrentlySelectedUri = ContentUris.withAppendedId(
				Constants.FAVORITE_CONTENT_URI, item.getId());

		mCurrentlySelectedOrigin = item.getOrigin();
		mCurrentlySelectedDestination = item.getDestination();

		startContextualActionMode();
	}

	@AfterViews
	void afterViews() {
		setTitle(R.string.favorite_routes);

		mRoutesAdapter = new FavoritesArrayAdapter(this,
				R.layout.favorite_listing);

		getSupportLoaderManager().initLoader(FAVORITES_LOADER_ID, null, this);

		setListAdapter(mRoutesAdapter);

		listView.setEmptyView(findViewById(android.R.id.empty));

		if (mCurrentAlerts != null) {
			showAlertMessage(mCurrentAlerts);
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			if (savedInstanceState.getString("currentlySelectedOrigin") != null) {
				mCurrentlySelectedOrigin = Station
						.getByAbbreviation(savedInstanceState
								.getString("currentlySelectedOrigin"));
			}
			if (savedInstanceState.getString("currentlySelectedDestination") != null) {
				mCurrentlySelectedDestination = Station
						.getByAbbreviation(savedInstanceState
								.getString("currentlySelectedDestination"));
			}
			if (savedInstanceState.getParcelable("currentlySelectedUri") != null) {
				mCurrentlySelectedUri = (Uri) savedInstanceState
						.getParcelable("currentlySelectedUri");
			}
			if (savedInstanceState.getBoolean("hasActionMode")) {
				startContextualActionMode();
			}
			mCurrentAlerts = savedInstanceState.getString("currentAlerts");
		}

		Ticker.getInstance().addSubscriber(this, getApplicationContext());
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, Constants.FAVORITE_CONTENT_URI,
				new String[] { RoutesColumns._ID.string,
						RoutesColumns.FROM_STATION.string,
						RoutesColumns.TO_STATION.string,
						RoutesColumns.FARE.string,
						RoutesColumns.FARE_LAST_UPDATED.string,
						RoutesColumns.AVERAGE_TRIP_SAMPLE_COUNT.string,
						RoutesColumns.AVERAGE_TRIP_LENGTH.string }, null, null,
				RoutesColumns._ID.string);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (cursor.getCount() == 0) {
			((TextView) findViewById(android.R.id.empty))
					.setText(R.string.empty_favorites_list_message);
		}
		mRoutesAdapter.updateFromCursor(cursor);
		refreshFares(cursor);
		findViewById(R.id.progress).setVisibility(View.GONE);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// Nothing to do
	}

	private AdapterView<ListAdapter> getListView() {
		return listView;
	}

	protected FavoritesArrayAdapter getListAdapter() {
		return mRoutesAdapter;
	}

	protected void setListAdapter(FavoritesArrayAdapter adapter) {
		mRoutesAdapter = adapter;
		getListView().setAdapter(mRoutesAdapter);
	}

	private void refreshFares(Cursor cursor) {
		if (cursor.moveToFirst()) {
			do {
				final Station orig = Station.getByAbbreviation(CursorUtils
						.getString(cursor, RoutesColumns.FROM_STATION));
				final Station dest = Station.getByAbbreviation(CursorUtils
						.getString(cursor, RoutesColumns.TO_STATION));
				final Long id = CursorUtils.getLong(cursor, RoutesColumns._ID);
				final Long lastUpdateMillis = CursorUtils.getLong(cursor,
						RoutesColumns.FARE_LAST_UPDATED);

				Calendar now = Calendar.getInstance();
				Calendar lastUpdate = Calendar.getInstance();
				lastUpdate.setTimeInMillis(lastUpdateMillis);

				now.setTimeZone(PACIFIC_TIME);
				lastUpdate.setTimeZone(PACIFIC_TIME);

				// Update every day
				if (now.get(Calendar.DAY_OF_YEAR) != lastUpdate
						.get(Calendar.DAY_OF_YEAR)) {
					GetRouteFareTask fareTask = new GetRouteFareTask() {
						@Override
						public void onResult(String fare) {
							ContentValues values = new ContentValues();
							values.put(RoutesColumns.FARE.string, fare);
							values.put(RoutesColumns.FARE_LAST_UPDATED.string,
									System.currentTimeMillis());

							getContentResolver()
									.update(ContentUris.withAppendedId(
											Constants.FAVORITE_CONTENT_URI, id),
											values, null, null);
						}

						@Override
						public void onError(Exception exception) {
							// Ignore... we can do this later
						}
					};
					fareTask.execute(new GetRouteFareTask.Params(orig, dest));
				}
			} while (cursor.moveToNext());
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mCurrentlySelectedOrigin != null)
			outState.putString("currentlySelectedOrigin",
					mCurrentlySelectedOrigin.abbreviation);
		if (mCurrentlySelectedDestination != null)
			outState.putString("currentlySelectedDestination",
					mCurrentlySelectedDestination.abbreviation);
		outState.putParcelable("currentlySelectedUri", mCurrentlySelectedUri);
		outState.putBoolean("hasActionMode", mActionMode != null);
		outState.putString("currentAlerts", mCurrentAlerts);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Ticker.getInstance().startTicking(this);
		if (mRoutesAdapter != null && !mRoutesAdapter.isEmpty()
				&& !mRoutesAdapter.areEtdListenersActive()) {
			mRoutesAdapter.setUpEtdListeners();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mRoutesAdapter != null && mRoutesAdapter.areEtdListenersActive()) {
			mRoutesAdapter.clearEtdListeners();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		Ticker.getInstance().stopTicking(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mRoutesAdapter != null) {
			mRoutesAdapter.close();
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			Ticker.getInstance().startTicking(this);
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.routes_list_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	private MenuItem elevatorMenuItem;
	private View origElevatorActionView;

	private String mCurrentAlerts;

	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.add_favorite_menu_button) {
			new AddRouteDialogFragment(getString(R.string.add_route))
					.show(getSupportFragmentManager().beginTransaction());
			return true;
		} else if (itemId == R.id.view_system_map_button) {
			startActivity(new Intent(this, ViewMapActivity.class));
			return true;
		} else if (itemId == R.id.elevator_button) {
			elevatorMenuItem = item;
			fetchElevatorInfo();
			origElevatorActionView = elevatorMenuItem.getActionView();
			elevatorMenuItem.setActionView(R.layout.progress_spinner);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Background
	void fetchAlerts() {
		Log.d(TAG, "Fetching alerts");
		AlertList alertList = alertsClient.getAlerts();
		if (alertList.hasAlerts()) {
			StringBuilder alertText = new StringBuilder();
			boolean firstAlert = true;
			for (Alert alert : alertList.getAlerts()) {
				if (!firstAlert) {
					alertText.append("\n\n");
				}
				alertText.append(alert.getPostedTime()).append("\n");
				alertText.append(alert.getDescription());
				firstAlert = false;
			}
			showAlertMessage(alertText.toString());
		} else if (alertList.areNoDelaysReported()) {
			showAlertMessage(NO_DELAYS_REPORTED);
		} else {
			hideAlertMessage();
		}
	}

	@UiThread
	void hideAlertMessage() {
		mCurrentAlerts = null;
		alertMessages.setVisibility(View.GONE);
	}

	@UiThread
	void showAlertMessage(String messageText) {
		if (messageText == null) {
			hideAlertMessage();
			return;
		} else if (messageText == NO_DELAYS_REPORTED) {
			alertMessages.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.ic_allgood, 0, 0, 0);
		} else {
			alertMessages.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.ic_warn, 0, 0, 0);
		}
		mCurrentAlerts = messageText;
		alertMessages.setText(messageText);
		alertMessages.setVisibility(View.VISIBLE);
	}

	@Background
	void fetchElevatorInfo() {
		String elevatorMessage = elevatorClient.getElevatorMessage();
		if (elevatorMessage != null) {
			showElevatorMessage(elevatorMessage);
		}
		resetElevatorMenuGraphic();
	}

	@UiThread
	void resetElevatorMenuGraphic() {
		invalidateOptionsMenu();
		elevatorMenuItem.setActionView(origElevatorActionView);
	}

	@UiThread
	void showElevatorMessage(String message) {
		Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message);
		builder.setTitle("Elevator status");
		builder.show();
	}

	private void startContextualActionMode() {
		mActionMode = startActionMode(new RouteActionMode());
		mActionMode.setTitle(mCurrentlySelectedOrigin.name);
		mActionMode.setSubtitle("to " + mCurrentlySelectedDestination.name);
	}

	private final class RouteActionMode implements ActionMode.Callback {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.route_context_menu, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (item.getItemId() == R.id.view) {
				startActivity(new Intent(Intent.ACTION_VIEW,
						mCurrentlySelectedUri));
				mode.finish();
				return true;
			} else if (item.getItemId() == R.id.delete) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(
						RoutesListActivity.this);
				builder.setCancelable(false);
				builder.setMessage("Are you sure you want to delete this route?");
				builder.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								getContentResolver().delete(
										mCurrentlySelectedUri, null, null);
								mCurrentlySelectedUri = null;
								mCurrentlySelectedOrigin = null;
								mCurrentlySelectedDestination = null;
								mActionMode.finish();
								dialog.dismiss();
							}
						});
				builder.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.cancel();
							}
						});
				builder.show();
				return false;
			}

			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
		}

	}

	@Override
	public int getTickInterval() {
		return 90;
	}

	@Override
	public void onTick(long mTickCount) {
		fetchAlerts();
	}
}