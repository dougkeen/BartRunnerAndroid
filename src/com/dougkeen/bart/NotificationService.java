package com.dougkeen.bart;

import java.util.List;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import com.dougkeen.bart.EtdService.EtdServiceBinder;
import com.dougkeen.bart.EtdService.EtdServiceListener;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.StationPair;

public class NotificationService extends IntentService implements
		EtdServiceListener {

	private static final int DEPARTURE_NOTIFICATION_ID = 123;
	private boolean mBound = false;
	private EtdService mEtdService;
	private Departure mDeparture;
	private StationPair mStationPair;
	private NotificationManager mNotificationManager;
	private AlarmManager mAlarmManager;
	private PendingIntent mNotificationIntent;
	private PendingIntent mAlarmPendingIntent;
	private int mAlertLeadTime;
	private Handler mHandler;

	public NotificationService() {
		super("BartRunnerNotificationService");
	}

	private final ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mEtdService = null;
			mBound = false;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mEtdService = ((EtdServiceBinder) service).getService();
			if (getStationPair() != null) {
				mEtdService.registerListener(NotificationService.this);
			}
			mBound = true;
		}
	};

	@Override
	public void onCreate() {
		bindService(new Intent(this, EtdService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		mHandler = new Handler();
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		if (mBound)
			unbindService(mConnection);
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle bundle = intent.getExtras();
		Departure departure = (Departure) bundle.getParcelable("departure");
		mStationPair = (StationPair) bundle.getParcelable("stationPair");
		mAlertLeadTime = bundle.getInt("alertLeadTime");

		if (mEtdService != null && mDeparture != null
				&& !mDeparture.equals(departure)) {
			mEtdService.unregisterListener(this);
		}

		mDeparture = departure;

		if (getStationPair() != null && mEtdService != null) {
			mEtdService.registerListener(this);
		}

		updateNotification();

		Intent targetIntent = new Intent(Intent.ACTION_VIEW,
				mStationPair.getUri());
		targetIntent.putExtra("boardedDeparture", mDeparture);
		targetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		mNotificationIntent = PendingIntent.getActivity(this, 0, targetIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		setAlarm();

		pollDepartureStatus();
	}

	private void refreshAlarmPendingIntent() {
		final Intent alarmIntent = new Intent(Constants.ACTION_ALARM,
				getStationPair().getUri());
		alarmIntent.putExtra("departure", mDeparture);
		mAlarmPendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private void setAlarm() {
		cancelAlarm();

		if (mAlertLeadTime > 0) {
			long alertTime = getAlertClockTime();
			if (alertTime > System.currentTimeMillis()) {
				refreshAlarmPendingIntent();
				mAlarmManager.set(AlarmManager.RTC_WAKEUP, alertTime,
						mAlarmPendingIntent);
			}
		}
	}

	private void triggerAlarmImmediately() {
		cancelAlarm();
		refreshAlarmPendingIntent();
		mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + 100, mAlarmPendingIntent);
	}

	private long getAlertClockTime() {
		return mDeparture.getMeanEstimate() - mAlertLeadTime * 60 * 1000;
	}

	private void cancelAlarm() {
		mAlarmManager.cancel(mAlarmPendingIntent);
	}

	@Override
	public void onETDChanged(List<Departure> departures) {
		for (Departure departure : departures) {
			if (departure.equals(mDeparture)
					&& (mDeparture.getMeanSecondsLeft() != departure
							.getMeanSecondsLeft() || mDeparture
							.getUncertaintySeconds() != departure
							.getUncertaintySeconds())) {
				long initialAlertClockTime = getAlertClockTime();

				mDeparture.mergeEstimate(departure);

				final long now = System.currentTimeMillis();
				if (initialAlertClockTime > now
						&& getAlertClockTime() <= System.currentTimeMillis()) {
					// Alert time was changed to the past
					triggerAlarmImmediately();
				} else if (getAlertClockTime() > System.currentTimeMillis()) {
					// Alert time is still in the future
					setAlarm();
				}

				break;
			}
		}
	}

	@Override
	public void onError(String errorMessage) {
		// Do nothing
	}

	@Override
	public void onRequestStarted() {
		// Do nothing
	}

	@Override
	public void onRequestEnded() {
		// Do nothing
	}

	@Override
	public StationPair getStationPair() {
		return mStationPair;
	}

	private long mNextScheduledCheckClockTime = 0;

	private void pollDepartureStatus() {
		if (mDeparture.hasDeparted()) {
			shutDown();
		}

		updateNotification();

		final int pollIntervalMillis = getPollIntervalMillis();
		final long scheduledCheckClockTime = System.currentTimeMillis()
				+ pollIntervalMillis;
		if (mNextScheduledCheckClockTime < scheduledCheckClockTime) {
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					pollDepartureStatus();
				}
			}, pollIntervalMillis);
			mNextScheduledCheckClockTime = scheduledCheckClockTime;
		}
	}

	private void shutDown() {
		mEtdService.unregisterListener(this);
		mNotificationManager.cancel(DEPARTURE_NOTIFICATION_ID);
		cancelAlarm();
		stopSelf();
	}

	private void updateNotification() {
		Builder notificationBuilder = new NotificationCompat.Builder(this);
		notificationBuilder.setOngoing(true);
		notificationBuilder.setSmallIcon(R.drawable.icon);
		final int minutes = mDeparture.getMeanSecondsLeft() / 60;
		final String minutesText = (minutes == 0) ? "Less than one minute"
				: (minutes + " minute" + ((minutes != 1) ? "s" : ""));
		notificationBuilder.setContentTitle(mStationPair.getDestination().name);
		notificationBuilder.setContentText(minutesText + " until departure");
		notificationBuilder.setContentIntent(mNotificationIntent);
		mNotificationManager.notify(DEPARTURE_NOTIFICATION_ID,
				notificationBuilder.getNotification());
	}

	private int getPollIntervalMillis() {
		final int secondsToAlarm = mDeparture.getMeanSecondsLeft()
				- mAlertLeadTime * 60;

		if (secondsToAlarm < -20) {
			/* Alarm should have already gone off by now */
			shutDown();
			return 10000000; // Arbitrarily large number
		}

		if (secondsToAlarm > 10 * 60) {
			return 60 * 1000;
		} else if (secondsToAlarm > 5 * 60) {
			return 60 * 1000;
		} else if (secondsToAlarm > 3 * 60) {
			return 30 * 1000;
		} else {
			return 10 * 1000;
		}
	}
}
