package com.dougkeen.bart;

import java.util.Calendar;
import java.util.TimeZone;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

import com.WazaBe.HoloEverywhere.AlertDialog;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.dougkeen.bart.data.CursorUtils;
import com.dougkeen.bart.data.RoutesColumns;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Station;
import com.dougkeen.bart.networktasks.GetRouteFareTask;

public class RoutesListActivity extends SherlockFragmentActivity {
	private static final TimeZone PACIFIC_TIME = TimeZone
			.getTimeZone("America/Los_Angeles");

	protected Cursor mQuery;

	private Uri mCurrentlySelectedUri;

	private Station mCurrentlySelectedOrigin;
	private Station mCurrentlySelectedDestination;

	private ActionMode mActionMode;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		setTitle(R.string.favorite_routes);

		mQuery = managedQuery(Constants.FAVORITE_CONTENT_URI, new String[] {
				RoutesColumns._ID.string, RoutesColumns.FROM_STATION.string,
				RoutesColumns.TO_STATION.string, RoutesColumns.FARE.string,
				RoutesColumns.FARE_LAST_UPDATED.string,
				RoutesColumns.AVERAGE_TRIP_SAMPLE_COUNT.string,
				RoutesColumns.AVERAGE_TRIP_LENGTH.string }, null, null,
				RoutesColumns._ID.string);

		refreshFares();

		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.favorite_listing, mQuery, new String[] {
						RoutesColumns.FROM_STATION.string,
						RoutesColumns.TO_STATION.string,
						RoutesColumns.FARE.string }, new int[] {
						R.id.originText, R.id.destinationText, R.id.fareText });
		adapter.setViewBinder(new ViewBinder() {
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {
				if (view.getId() == R.id.fareText) {
					String fare = cursor.getString(columnIndex);
					if (fare != null) {
						((TextView) view).setSingleLine(false);
						((TextView) view).setText("Fare:\n" + fare);
					}
				} else {
					((TextView) view).setText(Station.getByAbbreviation(cursor
							.getString(columnIndex)).name);
				}
				return true;
			}
		});

		setListAdapter(adapter);
		getListView().setOnItemClickListener(
				new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> l, View v,
							int position, long id) {
						startActivity(new Intent(Intent.ACTION_VIEW,
								ContentUris.withAppendedId(
										Constants.FAVORITE_CONTENT_URI, id)));
					}

				});
		getListView().setEmptyView(findViewById(android.R.id.empty));
		getListView().setOnItemLongClickListener(
				new AdapterView.OnItemLongClickListener() {
					@Override
					public boolean onItemLongClick(AdapterView<?> parent,
							View view, int position, long id) {
						if (mActionMode != null) {
							mActionMode.finish();
						}

						mCurrentlySelectedUri = ContentUris.withAppendedId(
								Constants.FAVORITE_CONTENT_URI, id);

						CursorWrapper item = (CursorWrapper) getListAdapter()
								.getItem(position);
						Station orig = Station.getByAbbreviation(CursorUtils
								.getString(item, RoutesColumns.FROM_STATION));
						Station dest = Station.getByAbbreviation(CursorUtils
								.getString(item, RoutesColumns.TO_STATION));
						mCurrentlySelectedOrigin = orig;
						mCurrentlySelectedDestination = dest;

						startContextualActionMode();
						return true;
					}
				});

		((Button) findViewById(R.id.quickLookupButton))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						DialogFragment dialog = new QuickRouteDialogFragment(
								getString(R.string.quick_departure_lookup));
						dialog.show(getSupportFragmentManager(), "dialog");
					}
				});

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
		}
	}

	@SuppressWarnings("unchecked")
	private AdapterView<ListAdapter> getListView() {
		return (AdapterView<ListAdapter>) findViewById(android.R.id.list);
	}

	private CursorAdapter mListAdapter;

	protected CursorAdapter getListAdapter() {
		return mListAdapter;
	}

	protected void setListAdapter(SimpleCursorAdapter adapter) {
		mListAdapter = adapter;
		getListView().setAdapter(mListAdapter);
	}

	private void refreshFares() {
		if (mQuery.moveToFirst()) {
			do {
				final Station orig = Station.getByAbbreviation(CursorUtils
						.getString(mQuery, RoutesColumns.FROM_STATION));
				final Station dest = Station.getByAbbreviation(CursorUtils
						.getString(mQuery, RoutesColumns.TO_STATION));
				final Long id = CursorUtils.getLong(mQuery, RoutesColumns._ID);
				final Long lastUpdateMillis = CursorUtils.getLong(mQuery,
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
			} while (mQuery.moveToNext());
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
	}

	@Override
	protected void onResume() {
		super.onResume();
		((TextView) findViewById(android.R.id.empty))
				.setText(R.string.empty_favorites_list_message);
		refreshFares();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.routes_list_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.add_favorite_menu_button) {
			new AddRouteDialogFragment(getString(R.string.add_route)).show(
					getSupportFragmentManager(), "dialog");
			return true;
		} else if (itemId == R.id.view_system_map_button) {
			startActivity(new Intent(this, ViewMapActivity.class));
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
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
}