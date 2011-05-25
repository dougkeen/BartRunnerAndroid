package com.dougkeen.bart.data;

import android.database.Cursor;

public final class CursorUtils {
	private CursorUtils() {
		// Static only class
	}

	public static final void closeCursorQuietly(Cursor cursor) {
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}

	public static final String getString(Cursor cursor, FavoritesColumns column) {
		return cursor.getString(cursor.getColumnIndex(column.string));
	}

}
