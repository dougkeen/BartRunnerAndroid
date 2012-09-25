package com.dougkeen.bart.data;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.dougkeen.bart.model.Constants;

public class BartContentProvider extends ContentProvider {

	private static final UriMatcher sUriMatcher;
	private static HashMap<String, String> sFavoritesProjectionMap;

	private static final int FAVORITES = 1;
	private static final int FAVORITE_ID = 2;
	private static final int ARBITRARY_ROUTE = 3;
	private static final int ARBITRARY_ROUTE_UNDEFINED = 4;

	/**
	 * The default sort order for events
	 */
	private static final String DEFAULT_SORT_ORDER = RoutesColumns.FROM_STATION.string
			+ " DESC";

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(Constants.AUTHORITY, "favorites", FAVORITES);
		sUriMatcher.addURI(Constants.AUTHORITY, "favorites/#", FAVORITE_ID);
		sUriMatcher.addURI(Constants.AUTHORITY, "route/*/*", ARBITRARY_ROUTE);
		sUriMatcher.addURI(Constants.AUTHORITY, "route",
				ARBITRARY_ROUTE_UNDEFINED);

		sFavoritesProjectionMap = new HashMap<String, String>();
		sFavoritesProjectionMap.put(RoutesColumns._ID.string,
				RoutesColumns._ID.string);
		sFavoritesProjectionMap.put(RoutesColumns.FROM_STATION.string,
				RoutesColumns.FROM_STATION.string);
		sFavoritesProjectionMap.put(RoutesColumns.TO_STATION.string,
				RoutesColumns.TO_STATION.string);
		sFavoritesProjectionMap.put(RoutesColumns.FARE.string,
				RoutesColumns.FARE.string);
		sFavoritesProjectionMap.put(RoutesColumns.FARE_LAST_UPDATED.string,
				RoutesColumns.FARE_LAST_UPDATED.string);
		sFavoritesProjectionMap.put(
				RoutesColumns.AVERAGE_TRIP_SAMPLE_COUNT.string,
				RoutesColumns.AVERAGE_TRIP_SAMPLE_COUNT.string);
		sFavoritesProjectionMap.put(RoutesColumns.AVERAGE_TRIP_LENGTH.string,
				RoutesColumns.AVERAGE_TRIP_LENGTH.string);
	}

	private DatabaseHelper mDatabaseHelper;

	@Override
	public boolean onCreate() {
		mDatabaseHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		int match = sUriMatcher.match(uri);
		if (match == FAVORITES) {
			return Constants.FAVORITE_CONTENT_TYPE;
		} else if (match == FAVORITE_ID) {
			return Constants.FAVORITE_CONTENT_ITEM_TYPE;
		} else if (match == ARBITRARY_ROUTE) {
			return Constants.ARBITRARY_ROUTE_TYPE;
		} else if (match == ARBITRARY_ROUTE_UNDEFINED) {
			return Constants.ARBITRARY_ROUTE_UNDEFINED_TYPE;
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();

		String orderBy = sortOrder;

		int match = sUriMatcher.match(uri);

		if (match == ARBITRARY_ROUTE) {
			final String origin = uri.getPathSegments().get(1);
			final String destination = uri.getPathSegments().get(2);

			qb.setTables(DatabaseHelper.FAVORITES_TABLE_NAME);
			qb.setProjectionMap(sFavoritesProjectionMap);
			qb.appendWhere(String.format("%s = '%s' AND %s = '%s'",
					RoutesColumns.FROM_STATION, origin,
					RoutesColumns.TO_STATION, destination));
			Cursor query = qb.query(db, projection, selection, selectionArgs,
					null, null, sortOrder);
			if (query.getCount() > 0)
				return query;

			MatrixCursor returnCursor = new MatrixCursor(projection);
			RowBuilder newRow = returnCursor.newRow();

			for (String column : projection) {
				if (column.equals(RoutesColumns.FROM_STATION.string)) {
					newRow.add(origin);
				} else if (column.equals(RoutesColumns.TO_STATION.string)) {
					newRow.add(destination);
				} else {
					newRow.add(null);
				}
			}

			return returnCursor;
		} else if (match == FAVORITE_ID) {
			qb.setTables(DatabaseHelper.FAVORITES_TABLE_NAME);
			qb.setProjectionMap(sFavoritesProjectionMap);
			qb.appendWhere(RoutesColumns._ID + " = "
					+ uri.getPathSegments().get(1));
		} else if (match == FAVORITES) {
			qb.setTables(DatabaseHelper.FAVORITES_TABLE_NAME);
			qb.setProjectionMap(sFavoritesProjectionMap);
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		if (TextUtils.isEmpty(orderBy)) {
			orderBy = DEFAULT_SORT_ORDER;
		}

		// Get the database and run the query
		Cursor cursor = qb.query(db, projection, selection, selectionArgs,
				null, null, orderBy);

		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

		// Validate the requested uri
		int match = sUriMatcher.match(uri);
		if (match == FAVORITES) {
			long rowId = -1;
			Cursor cursor = db
					.query(DatabaseHelper.FAVORITES_TABLE_NAME,
							new String[] { RoutesColumns._ID.string },
							RoutesColumns.FROM_STATION + "=? AND "
									+ RoutesColumns.TO_STATION + "=?",
							new String[] {
									values.getAsString(RoutesColumns.FROM_STATION.string),
									values.getAsString(RoutesColumns.TO_STATION.string) },
							null, null, null);
			try {
				if (cursor.moveToFirst()) {
					rowId = cursor.getLong(0);
				}
			} finally {
				CursorUtils.closeCursorQuietly(cursor);
			}
			if (rowId < 0) {
				rowId = db.insert(DatabaseHelper.FAVORITES_TABLE_NAME,
						RoutesColumns.FROM_STATION.string, values);
			}
			if (rowId > 0) {
				Uri eventUri = ContentUris.withAppendedId(
						Constants.FAVORITE_CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(eventUri, null,
						false);
				return eventUri;
			}
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

		// Validate the requested uri
		int match = sUriMatcher.match(uri);
		if (match == FAVORITE_ID) {
			String favoriteId = uri.getPathSegments().get(1);
			int count = db.update(
					DatabaseHelper.FAVORITES_TABLE_NAME,
					values,
					RoutesColumns._ID
							+ " = "
							+ favoriteId
							+ (!TextUtils.isEmpty(where) ? " AND (" + where
									+ ')' : ""), whereArgs);
			getContext().getContentResolver().notifyChange(uri, null);
			return count;
		} else if (match == ARBITRARY_ROUTE) {
			// Get the route with the origin and destination provided, and
			// simply delegate to the previous log branch. If the given route
			// doesn't exist, do nothing.
			String origin = uri.getPathSegments().get(1);
			String destination = uri.getPathSegments().get(2);

			Cursor query = db.query(DatabaseHelper.FAVORITES_TABLE_NAME,
					new String[] { RoutesColumns._ID.string },
					RoutesColumns.FROM_STATION.string + "=? AND "
							+ RoutesColumns.TO_STATION.string + "=?",
					new String[] { origin, destination }, null, null, null);

			try {
				if (query.moveToFirst()) {
					return update(ContentUris.withAppendedId(
							Constants.FAVORITE_CONTENT_URI, query.getLong(0)),
							values, where, whereArgs);
				}
			} finally {
				CursorUtils.closeCursorQuietly(query);
			}
		}
		return 0;
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		// TODO: Sync with REST service?
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		int count;
		int match = sUriMatcher.match(uri);
		if (match == FAVORITES) {
			count = db.delete(DatabaseHelper.FAVORITES_TABLE_NAME, where,
					whereArgs);
		} else if (match == FAVORITE_ID) {
			String favoriteId = uri.getPathSegments().get(1);
			count = db.delete(
					DatabaseHelper.FAVORITES_TABLE_NAME,
					RoutesColumns._ID
							+ " = "
							+ favoriteId
							+ (!TextUtils.isEmpty(where) ? " AND (" + where
									+ ')' : ""), whereArgs);
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
}
