package com.dougkeen.bart;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.dougkeen.bart.activities.ViewDeparturesActivity;
import com.dougkeen.bart.data.DatabaseHelper;
import com.dougkeen.bart.data.FavoritesPersistence;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.Station;
import com.dougkeen.bart.model.StationPair;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EApplication;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.sentry.Sentry;
import io.sentry.android.AndroidSentryClientFactory;

@EApplication
public class BartRunnerApplication extends Application implements
        Application.ActivityLifecycleCallbacks {

    private static final int FIVE_MINUTES = 5 * 60 * 1000;

    private static final String CACHE_FILE_NAME = "lastBoardedDeparture";
    private static final String PREFS_NAME = "prefs_bart_runner";
    private static final String PREFS_ACTIVITY_TIMESTAMP = "prefs_activity_timestamp";

    private Departure mBoardedDeparture;

    private boolean mPlayAlarmRingtone;

    private boolean mAlarmSounding;

    private MediaPlayer mAlarmMediaPlayer;

    private SharedPreferences mApplicationPreferences;

    private static Context context;

    @Bean
    FavoritesPersistence favoritesPersistenceContext;

    private List<StationPair> favorites;

    public void saveFavorites() {
        if (favorites != null) {
            favoritesPersistenceContext.persist(favorites);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                createAppShortcuts(favorites);
            }
        }
    }

    public List<StationPair> getFavorites() {
        if (favorites == null) {
            favorites = favoritesPersistenceContext.restore();
            if (favorites.isEmpty()) {
                // Upgrade database, in case favorites are still in there
                new DatabaseHelper(this).getReadableDatabase().close();
                favorites = favoritesPersistenceContext.restore();
            }
        }
        return favorites;
    }

    public void setFavorites(List<StationPair> favorites) {
        this.favorites = favorites;
    }

    public StationPair getFavorite(Station origin, Station destination) {
        for (StationPair favorite : getFavorites()) {
            if (origin.equals(favorite.getOrigin())
                    && destination.equals(favorite.getDestination())) {
                return favorite;
            }
        }
        return null;
    }

    public void addFavorite(StationPair favorite) {
        getFavorites().add(favorite);
        saveFavorites();
    }

    public void removeFavorite(StationPair favorite) {
        getFavorites().remove(favorite);
        saveFavorites();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        mApplicationPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        registerActivityLifecycleCallbacks(this);

        Sentry.init("https://cb2a611cee27437c95ee725e1f373137@sentry.io/1325487", new AndroidSentryClientFactory(context));
    }

    public static Context getAppContext() {
        return context;
    }

    public boolean shouldPlayAlarmRingtone() {
        return mPlayAlarmRingtone;
    }

    public void setPlayAlarmRingtone(boolean playAlarmRingtone) {
        this.mPlayAlarmRingtone = playAlarmRingtone;
    }

    public Departure getBoardedDeparture() {
        return getBoardedDeparture(false);
    }

    public Departure getBoardedDeparture(boolean useOldCache) {
        if (mBoardedDeparture == null) {
            // see if there's a saved one
            File cachedDepartureFile = new File(getCacheDir(), CACHE_FILE_NAME);
            if (cachedDepartureFile.exists()) {
                InputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(cachedDepartureFile);
                    final byte[] byteArray = IOUtils.toByteArray(inputStream);
                    final Parcel parcel = Parcel.obtain();
                    parcel.unmarshall(byteArray, 0, byteArray.length);
                    parcel.setDataPosition(0);
                    Departure lastBoardedDeparture = Departure.CREATOR
                            .createFromParcel(parcel);
                    parcel.recycle();

                    /*
                     * Ooptionally check if the cached one is relatively recent.
                     * If so, restore that to the application context
                     */
                    long now = System.currentTimeMillis();
                    if (useOldCache
                            || lastBoardedDeparture.getEstimatedArrivalTime() >= now
                            - FIVE_MINUTES
                            || lastBoardedDeparture.getMeanEstimate() >= now
                            - 2 * FIVE_MINUTES) {
                        mBoardedDeparture = lastBoardedDeparture;
                    }
                } catch (Exception e) {
                    Log.w(Constants.TAG,
                            "Couldn't read or unmarshal lastBoardedDeparture file",
                            e);
                    try {
                        cachedDepartureFile.delete();
                    } catch (SecurityException anotherException) {
                        Log.w(Constants.TAG,
                                "Couldn't delete lastBoardedDeparture file",
                                anotherException);
                    }
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }
        }
        if (mBoardedDeparture != null && mBoardedDeparture.hasExpired()) {
            setBoardedDeparture(null);
        }
        return mBoardedDeparture;
    }

    public void setBoardedDeparture(Departure boardedDeparture) {
        if (!ObjectUtils.equals(boardedDeparture, mBoardedDeparture)
                || ObjectUtils.compare(mBoardedDeparture, boardedDeparture) != 0) {
            if (this.mBoardedDeparture != null) {
                this.mBoardedDeparture.getAlarmLeadTimeMinutesObservable()
                        .unregisterAllObservers();
                this.mBoardedDeparture.getAlarmPendingObservable()
                        .unregisterAllObservers();

                // Cancel any pending alarms for the current departure
                if (this.mBoardedDeparture.isAlarmPending()) {
                    this.mBoardedDeparture
                            .cancelAlarm(
                                    this,
                                    (AlarmManager) getSystemService(Context.ALARM_SERVICE));
                }
            }

            this.mBoardedDeparture = boardedDeparture;

            File cachedDepartureFile = new File(getCacheDir(), CACHE_FILE_NAME);
            if (mBoardedDeparture == null) {
                try {
                    cachedDepartureFile.delete();
                } catch (SecurityException anotherException) {
                    Log.w(Constants.TAG,
                            "Couldn't delete lastBoardedDeparture file",
                            anotherException);
                }
            } else {
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(cachedDepartureFile);
                    Parcel parcel = Parcel.obtain();
                    mBoardedDeparture.writeToParcel(parcel, 0);
                    fileOutputStream.write(parcel.marshall());
                } catch (Exception e) {
                    Log.w(Constants.TAG,
                            "Couldn't write last boarded departure cache file",
                            e);
                } finally {
                    IOUtils.closeQuietly(fileOutputStream);
                }
            }
        }
    }

    public boolean isAlarmSounding() {
        return mAlarmSounding;
    }

    public void setAlarmSounding(boolean alarmSounding) {
        this.mAlarmSounding = alarmSounding;
    }

    public MediaPlayer getAlarmMediaPlayer() {
        return mAlarmMediaPlayer;
    }

    public void setAlarmMediaPlayer(MediaPlayer alarmMediaPlayer) {
        this.mAlarmMediaPlayer = alarmMediaPlayer;
    }

    public void setActivityTimestamp(long timestamp) {
        mApplicationPreferences.edit().putLong(PREFS_ACTIVITY_TIMESTAMP, timestamp).apply();
    }

    public long getActivityTimestamp() {
        return mApplicationPreferences.getLong(PREFS_ACTIVITY_TIMESTAMP, 0L);
    }

    /**
     * Make the first 4 favorite routes into app shortcuts. Save room for the
     * static map shortcut.
     *
     * @param favorites The user's saved routes
     */
    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private void createAppShortcuts(List<StationPair> favorites) {
        List<ShortcutInfo> shortcuts = new ArrayList<>();
        for (int i=0; i<favorites.size() && i<4; i++) {
            StationPair favorite = favorites.get(i);
            Icon icon = Icon.createWithResource(this, R.drawable.shortcut_station);
            shortcuts.add(new ShortcutInfo.Builder(this, favorite.toString())
                    .setShortLabel(getString(R.string.station_pair_description,
                            favorite.getOrigin().shortName,
                            favorite.getDestination().shortName))
                    .setIcon(icon)
                    .setIntent(new Intent(this, ViewDeparturesActivity.class)
                            .setAction(Intent.ACTION_VIEW)
                            .putExtra(ViewDeparturesActivity.EXTRA_ORIGIN, favorite.getOrigin().abbreviation)
                            .putExtra(ViewDeparturesActivity.EXTRA_DESTINATION, favorite.getDestination().abbreviation))
                    .build());
        }
        ShortcutManager shortcutManager = (ShortcutManager) getSystemService(Context.SHORTCUT_SERVICE);
        shortcutManager.setDynamicShortcuts(shortcuts);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        setActivityTimestamp(System.currentTimeMillis());
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
