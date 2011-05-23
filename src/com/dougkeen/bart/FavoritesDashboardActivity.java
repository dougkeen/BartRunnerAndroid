package com.dougkeen.bart;

import com.dougkeen.bart.data.FavoritesColumns;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class FavoritesDashboardActivity extends ListActivity {
	protected Cursor mQuery;

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
}