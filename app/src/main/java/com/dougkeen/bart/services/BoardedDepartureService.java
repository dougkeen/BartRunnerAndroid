package com.dougkeen.bart.services;

import java.lang.ref.WeakReference;
import java.util.List;

import android.app.AlarmManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationManagerCompat;

import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.StationPair;
import com.dougkeen.bart.services.EtdService.EtdServiceBinder;
import com.dougkeen.bart.services.EtdService.EtdServiceListener;
import com.dougkeen.util.Observer;

public class BoardedDepartureService extends Service implements
        EtdServiceListener {

    private static final int DEPARTURE_NOTIFICATION_ID = 123;

    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

    private boolean mBound = false;
    private EtdService mEtdService;
    private StationPair mStationPair;
    private NotificationManagerCompat mNotificationManager;
    private AlarmManager mAlarmManager;
    private Handler mHandler;
    private boolean mHasShutDown = false;

    public BoardedDepartureService() {
        super();
    }

    private static final class ServiceHandler extends Handler {
        private final WeakReference<BoardedDepartureService> mServiceRef;

        public ServiceHandler(Looper looper,
                              BoardedDepartureService boardedDepartureService) {
            super(looper);
            mServiceRef = new WeakReference<BoardedDepartureService>(
                    boardedDepartureService);
        }

        @Override
        public void handleMessage(Message msg) {
            BoardedDepartureService service = mServiceRef.get();
            if (service != null) {
                service.onHandleIntent((Intent) msg.obj);
            }
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
                mEtdService.registerListener(BoardedDepartureService.this,
                        false);
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
        mServiceHandler = new ServiceHandler(mServiceLooper, this);

        bindService(EtdService_.intent(this).get(), mConnection,
                Context.BIND_AUTO_CREATE);
        mNotificationManager = NotificationManagerCompat.from(this);
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
        mHasShutDown = false;
        onStart(intent, startId);
        return START_REDELIVER_INTENT;
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
        if (intent == null)
            return;

        final BartRunnerApplication application = (BartRunnerApplication) getApplication();
        final Departure boardedDeparture;
        if (intent.hasExtra("departure")) {
            boardedDeparture = intent.getExtras().getParcelable("departure");
        } else {
            boardedDeparture = application.getBoardedDeparture();
        }
        if (boardedDeparture == null) {
            // Nothing to notify about
            if (mNotificationManager != null) {
                mNotificationManager.cancel(DEPARTURE_NOTIFICATION_ID);
            }
            return;
        }
        if (intent.getBooleanExtra("cancelNotifications", false)
                || intent.getBooleanExtra("clearBoardedDeparture", false)) {
            // We want to cancel the alarm
            boardedDeparture
                    .cancelAlarm(getApplicationContext(), mAlarmManager);
            if (intent.getBooleanExtra("clearBoardedDeparture", false)) {
                application.setBoardedDeparture(null);
                shutDown(false);
            } else {
                updateNotification();
            }
            return;
        }

        StationPair oldStationPair = mStationPair;
        mStationPair = boardedDeparture.getStationPair();

        if (mEtdService != null && mStationPair != null
                && !mStationPair.equals(oldStationPair)) {
            mEtdService.unregisterListener(this);
        }

        if (getStationPair() != null && mEtdService != null) {
            mEtdService.registerListener(this, false);
        }

        boardedDeparture.getAlarmLeadTimeMinutesObservable().registerObserver(
                new Observer<Integer>() {
                    @Override
                    public void onUpdate(Integer newValue) {
                        updateNotification();
                    }
                });
        boardedDeparture.getAlarmPendingObservable().registerObserver(
                new Observer<Boolean>() {
                    @Override
                    public void onUpdate(Boolean newValue) {
                        updateNotification();
                    }
                });

        updateNotification();

        pollDepartureStatus();
    }

    private void updateAlarm() {
        Departure boardedDeparture = ((BartRunnerApplication) getApplication())
                .getBoardedDeparture();
        if (boardedDeparture != null) {
            boardedDeparture
                    .updateAlarm(getApplicationContext(), mAlarmManager);
        }
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
                boardedDeparture.mergeEstimate(departure);
                // Also merge back, in case boardedDeparture estimate is better
                departure.mergeEstimate(boardedDeparture);

                updateAlarm();
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

        if (boardedDeparture == null || boardedDeparture.hasDeparted()) {
            shutDown(false);
            return;
        }

        if (mEtdService != null) {
            /*
             * Make sure we're still listening for ETD changes (in case weak ref
             * was garbage collected). Not a huge fan of this approach, but I
             * think I'd rather keep the weak references to avoid memory leaks
             * than move to soft references or some other form of stronger
             * reference. Besides, registerListener() should only result in a
             * few constant-time map operations, so there shouldn't be a big
             * performance hit.
             */
            mEtdService.registerListener(this, false);
        }

        boardedDeparture.updateAlarm(getApplicationContext(), mAlarmManager);

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
            if (mEtdService != null) {
                mEtdService.unregisterListener(this);
            }
            if (mNotificationManager != null) {
                mNotificationManager.cancel(DEPARTURE_NOTIFICATION_ID);
            }
            if (!isBeingDestroyed)
                stopSelf();
        }
    }

    private void updateNotification() {
        if (mHasShutDown) {
            if (mEtdService != null) {
                mEtdService.unregisterListener(this);
            }
            return;
        }

        final Departure boardedDeparture = ((BartRunnerApplication) getApplication())
                .getBoardedDeparture();
        if (boardedDeparture != null) {
            mNotificationManager.notify(DEPARTURE_NOTIFICATION_ID,
                    boardedDeparture.createNotification(getApplicationContext()));
        }
    }

    private int getPollIntervalMillis() {
        final Departure boardedDeparture = ((BartRunnerApplication) getApplication())
                .getBoardedDeparture();

        if (boardedDeparture.getSecondsUntilAlarm() > 3 * 60) {
            return 15 * 1000;
        } else {
            return 6 * 1000;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Doesn't support binding
        return null;
    }

}
