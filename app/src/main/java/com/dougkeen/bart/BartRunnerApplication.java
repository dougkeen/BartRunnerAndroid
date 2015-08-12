package com.dougkeen.bart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;

import android.app.AlarmManager;
import android.app.Application;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Parcel;
import android.util.Log;

import com.dougkeen.bart.data.DatabaseHelper;
import com.dougkeen.bart.data.FavoritesPersistence;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.Station;
import com.dougkeen.bart.model.StationPair;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EApplication;

@EApplication
public class BartRunnerApplication extends Application {
    private static final int FIVE_MINUTES = 5 * 60 * 1000;

    private static final String CACHE_FILE_NAME = "lastBoardedDeparture";

    private Departure mBoardedDeparture;

    private boolean mPlayAlarmRingtone;

    private boolean mAlarmSounding;

    private MediaPlayer mAlarmMediaPlayer;

    private static Context context;

    @Bean
    FavoritesPersistence favoritesPersistenceContext;

    private List<StationPair> favorites;

    public void saveFavorites() {
        if (favorites != null) {
            favoritesPersistenceContext.persist(favorites);
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
}
