package com.dougkeen.bart;

import java.util.List;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import com.dougkeen.bart.EtdService.EtdServiceBinder;
import com.dougkeen.bart.EtdService.EtdServiceListener;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.StationPair;

public class NotificationService extends Service implements EtdServiceListener {

	private static final int DEPARTURE_NOTIFICATION_ID = 123;

	private volatile Looper mServiceLooper;
	private volatile ServiceHandler mServiceHandler;

	private boolean mBound = false;
	private EtdService mEtdService;
	private StationPair mStationPair;
	private NotificationManager mNotificationManager;
	private AlarmManager mAlarmManager;
	private PendingIntent mNotificationIntent;
	private PendingIntent mAlarmPendingIntent;
	private int mAlertLeadTime;
	private Handler mHandler;
	private boolean mHasShutDown = false;

	public NotificationService() {
		super();
	}

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			onHandleIntent((Intent) msg.obj);
		}
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
		HandlerThread thread = new HandlerThread(
				"BartRunnerNotificationService");
		thread.start();

		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

		bindService(new Intent(this, EtdService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		mHandler = new Handler();
		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		msg.obj = intent;
		mServiceHandler.sendMessage(msg);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		onStart(intent, startId);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		shutDown(true);
		if (mBound)
			unbindService(mConnection);
		mServiceLooper.quit();
		super.onDestroy();
	}

	protected void onHandleIntent(Intent intent) {
		final Departure boardedDeparture = ((BartRunnerApplication) getApplication())
				.getBoardedDeparture();
		if (boardedDeparture == null) {
			// Nothing to notify about
			shutDown(false);
			return;
		}

		Bundle bundle = intent.getExtras();
		StationPair oldStationPair = mStationPair;
		mStationPair = boardedDeparture.getStationPair();
		mAlertLeadTime = bundle.getInt("alertLeadTime");

		if (mEtdService != null && mStationPair != null
				&& !mStationPair.equals(oldStationPair)) {
			mEtdService.unregisterListener(this);
		}

		if (getStationPair() != null && mEtdService != null) {
			mEtdService.registerListener(this);
		}

		updateNotification();

		Intent targetIntent = new Intent(Intent.ACTION_VIEW,
				mStationPair.getUri());
		targetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		mNotificationIntent = PendingIntent.getActivity(this, 0, targetIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		setAlarm();

		pollDepartureStatus();
	}

	private void refreshAlarmPendingIntent() {
		final Intent alarmIntent = new Intent(Constants.ACTION_ALARM,
				getStationPair().getUri());
		final Departure boardedDeparture = ((BartRunnerApplication) getApplication())
				.getBoardedDeparture();
		alarmIntent.putExtra("departure", boardedDeparture);
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
		final Departure boardedDeparture = ((BartRunnerApplication) getApplication())
				.getBoardedDeparture();
		return boardedDeparture.getMeanEstimate() - mAlertLeadTime * 60 * 1000;
	}

	private void cancelAlarm() {
		mAlarmManager.cancel(mAlarmPendingIntent);
	}

	@Override
	public void onETDChanged(List<Departure> departures) {
		final Departure boardedDeparture = ((BartRunnerApplication) getApplication())
				.getBoardedDeparture();
		for (Departure departure : departures) {
			if (departure.equals(boardedDeparture)
					&& (boardedDeparture.getMeanSecondsLeft() != departure
							.getMeanSecondsLeft() || boardedDeparture
							.getUncertaintySeconds() != departure
							.getUncertaintySeconds())) {
				long initialAlertClockTime = getAlertClockTime();

				boardedDeparture.mergeEstimate(departure);

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
		final Departure boardedDeparture = ((BartRunnerApplication) getApplication())
				.getBoardedDeparture();

		if (boardedDeparture.hasDeparted()) {
			shutDown(false);
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

	private void shutDown(boolean isBeingDestroyed) {
		if (!mHasShutDown) {
			mHasShutDown = true;
			mEtdService.unregisterListener(this);
			mNotificationManager.cancel(DEPARTURE_NOTIFICATION_ID);
			cancelAlarm();
			if (!isBeingDestroyed)
				stopSelf();
		}
	}

	private void updateNotification() {
		if (mHasShutDown) {
			mNotificationManager.cancel(DEPARTURE_NOTIFICATION_ID);
			return;
		}

		final Departure boardedDeparture = ((BartRunnerApplication) getApplication())
				.getBoardedDeparture();
		final int halfMinutes = (boardedDeparture.getMeanSecondsLeft() + 15) / 30;
		float minutes = halfMinutes / 2f;
		final String minutesText = (minutes < 1) ? "Less than one minute"
				: (String.format("~%.1f minute", minutes) + ((minutes != 1.0) ? "s"
						: ""));

		Builder notificationBuilder = new NotificationCompat.Builder(this)
				.setOngoing(true)
				.setSmallIcon(R.drawable.icon)
				.setContentTitle(
						mStationPair.getOrigin().shortName + " to "
								+ mStationPair.getDestination().shortName)
				.setContentText(minutesText + " until departure")
				.setContentIntent(mNotificationIntent);
		mNotificationManager.notify(DEPARTURE_NOTIFICATION_ID,
				notificationBuilder.getNotification());
	}

	private int getPollIntervalMillis() {
		final Departure boardedDeparture = ((BartRunnerApplication) getApplication())
				.getBoardedDeparture();
		final int secondsToAlarm = boardedDeparture.getMeanSecondsLeft()
				- mAlertLeadTime * 60;

		if (secondsToAlarm < -20) {
			/* Alarm should have already gone off by now */
			shutDown(false);
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

	@Override
	public IBinder onBind(Intent intent) {
		// Doesn't support binding
		return null;
	}
}
