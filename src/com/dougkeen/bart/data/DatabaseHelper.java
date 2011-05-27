package com.dougkeen.bart.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "bart.dougkeen.db";
	private static final int DATABASE_VERSION = 1;

	public static final String FAVORITES_TABLE_NAME = "Favorites";

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + FAVORITES_TABLE_NAME + " (" +
				RoutesColumns._ID.getColumnDef() + " PRIMARY KEY, " +
				RoutesColumns.FROM_STATION.getColumnDef() + ", " +
				RoutesColumns.TO_STATION.getColumnDef() + ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
}
