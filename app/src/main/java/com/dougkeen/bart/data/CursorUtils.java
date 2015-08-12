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

    public static final String getString(Cursor cursor, RoutesColumns column) {
        return cursor.getString(cursor.getColumnIndex(column.string));
    }

    public static final Long getLong(Cursor cursor, RoutesColumns column) {
        return cursor.getLong(cursor.getColumnIndex(column.string));
    }

    public static final Integer getInteger(Cursor cursor, RoutesColumns column) {
        return cursor.getInt(cursor.getColumnIndex(column.string));
    }
}
