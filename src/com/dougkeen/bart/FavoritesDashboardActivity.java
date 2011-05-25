package com.dougkeen.bart;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

import com.dougkeen.bart.data.CursorUtils;
import com.dougkeen.bart.data.FavoritesColumns;

public class FavoritesDashboardActivity extends ListActivity {
	private static final int DIALOG_DELETE_EVENT = 0;

	protected Cursor mQuery;

	private Uri mCurrentlySelectedUri;

	private String mCurrentlySelectedRouteName;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		((TextView) findViewById(R.id.listTitle))
				.setText(R.string.favorite_routes);
		((TextView) findViewById(android.R.id.empty))
				.setText(R.string.empty_favorites_list_message);

		mQuery = managedQuery(Constants.FAVORITE_CONTENT_URI, new String[] {
				FavoritesColumns._ID.string,
				FavoritesColumns.FROM_STATION.string,
				FavoritesColumns.TO_STATION.string }, null, null,
				FavoritesColumns._ID.string);

		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.favorite_listing,
				mQuery,
				new String[] { FavoritesColumns.FROM_STATION.string,
								FavoritesColumns.TO_STATION.string },
				new int[] { R.id.originText,
							R.id.destinationText });
		adapter.setViewBinder(new ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {
				((TextView) view).setText(Station.getByAbbreviation(cursor
						.getString(columnIndex)).name);
				return true;
			}
		});

		setListAdapter(adapter);

		registerForContextMenu(getListView());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.favorites_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.add_favorite_menu_button) {
			startActivity(new Intent(Intent.ACTION_INSERT,
					Constants.FAVORITE_CONTENT_URI));
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		startActivity(new Intent(Intent.ACTION_VIEW,
				ContentUris.withAppendedId(Constants.FAVORITE_CONTENT_URI, id)));
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.favorite_context_menu, menu);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		CursorWrapper item = (CursorWrapper) getListAdapter().getItem(
				info.position);
		Station orig = Station.getByAbbreviation(CursorUtils.getString(item,
				FavoritesColumns.FROM_STATION));
		Station dest = Station.getByAbbreviation(CursorUtils.getString(item,
				FavoritesColumns.TO_STATION));
		mCurrentlySelectedRouteName = orig.name + " - " + dest.name;
		menu.setHeaderTitle(mCurrentlySelectedRouteName);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		mCurrentlySelectedUri = ContentUris.withAppendedId(
				Constants.FAVORITE_CONTENT_URI,
				info.id);

		if (item.getItemId() == R.id.view) {
			startActivity(new Intent(Intent.ACTION_VIEW, mCurrentlySelectedUri));
			return true;
		} else if (item.getItemId() == R.id.delete) {
			showDialog(DIALOG_DELETE_EVENT);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_DELETE_EVENT && mCurrentlySelectedUri != null) {
			final AlertDialog.Builder builder = new Builder(this);
			builder.setTitle(mCurrentlySelectedRouteName);
			builder.setCancelable(false);
			builder.setMessage("Are you sure you want to delete this route?");
			builder.setPositiveButton(R.string.yes,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							getContentResolver().delete(mCurrentlySelectedUri,
									null, null);
							mCurrentlySelectedUri = null;
							mCurrentlySelectedRouteName = null;
							removeDialog(DIALOG_DELETE_EVENT);
						}
					});
			builder.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mCurrentlySelectedUri = null;
							mCurrentlySelectedRouteName = null;
							removeDialog(DIALOG_DELETE_EVENT);
						}
					});
			return builder.create();
		}
		return super.onCreateDialog(id);
	}

}