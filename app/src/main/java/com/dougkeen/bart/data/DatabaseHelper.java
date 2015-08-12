package com.dougkeen.bart.data;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.model.StationPair;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bart.dougkeen.db";
    private static final int DATABASE_VERSION = 6;

    public static final String FAVORITES_TABLE_NAME = "Favorites";

    private BartRunnerApplication app;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        app = (BartRunnerApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    private void createFavoritesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + FAVORITES_TABLE_NAME + " ("
                + RoutesColumns._ID.getColumnDef() + " PRIMARY KEY, "
                + RoutesColumns.FROM_STATION.getColumnDef() + ", "
                + RoutesColumns.TO_STATION.getColumnDef() + ", "
                + RoutesColumns.FARE.getColumnDef() + ", "
                + RoutesColumns.FARE_LAST_UPDATED.getColumnDef() + ", "
                + RoutesColumns.AVERAGE_TRIP_SAMPLE_COUNT.getColumnDef() + ", "
                + RoutesColumns.AVERAGE_TRIP_LENGTH.getColumnDef() + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            createFavoritesTable(db);

            Cursor query = db.query(FAVORITES_TABLE_NAME, RoutesColumns.all(),
                    null, null, null, null, null);

            List<StationPair> favorites = new ArrayList<StationPair>();

            while (query.moveToNext()) {
                favorites.add(StationPair.createFromCursor(query));
            }

            query.close();

            new FavoritesPersistence(app).persist(favorites);

            db.execSQL("DROP TABLE " + FAVORITES_TABLE_NAME);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
