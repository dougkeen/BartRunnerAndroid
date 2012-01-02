package com.dougkeen.bart.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "bart.dougkeen.db";
	private static final int DATABASE_VERSION = 2;

	public static final String FAVORITES_TABLE_NAME = "Favorites";

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createFavoritesTable(db);
	}

	private void createFavoritesTable(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS " + FAVORITES_TABLE_NAME + " ("
				+ RoutesColumns._ID.getColumnDef() + " PRIMARY KEY, "
				+ RoutesColumns.FROM_STATION.getColumnDef() + ", "
				+ RoutesColumns.TO_STATION.getColumnDef() + ", "
				+ RoutesColumns.FARE.getColumnDef() + ", "
				+ RoutesColumns.FARE_LAST_UPDATED.getColumnDef() + ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.beginTransaction();
		try {
			createFavoritesTable(db);

			List<String> columns = getColumns(db, FAVORITES_TABLE_NAME);

			db.execSQL("ALTER TABLE " + FAVORITES_TABLE_NAME
					+ " RENAME TO temp_" + FAVORITES_TABLE_NAME);

			createFavoritesTable(db);

			columns.retainAll(getColumns(db, FAVORITES_TABLE_NAME));

			String cols = StringUtils.join(columns, ",");
			db.execSQL(String.format(
					"INSERT INTO %s (%s) SELECT %s from temp_%s",
					FAVORITES_TABLE_NAME, cols, cols, FAVORITES_TABLE_NAME));

			db.execSQL("DROP TABLE temp_" + FAVORITES_TABLE_NAME);

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public static List<String> getColumns(SQLiteDatabase db, String tableName) {
		List<String> ar = null;
		Cursor c = null;
		try {
			c = db.rawQuery("select * from " + tableName + " limit 1", null);
			if (c != null) {
				ar = new ArrayList<String>(Arrays.asList(c.getColumnNames()));
			}
		} catch (Exception e) {
			Log.v(tableName, e.getMessage(), e);
			e.printStackTrace();
		} finally {
			if (c != null)
				c.close();
		}
		return ar;
	}

	public static String join(List<String> list, String delim) {
		StringBuilder buf = new StringBuilder();
		int num = list.size();
		for (int i = 0; i < num; i++) {
			if (i != 0)
				buf.append(delim);
			buf.append((String) list.get(i));
		}
		return buf.toString();
	}
}
