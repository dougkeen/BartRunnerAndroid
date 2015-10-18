package com.dougkeen.bart.activities;

import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.Vibrator;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.text.format.DateFormat;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Checkable;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.R;
import com.dougkeen.bart.controls.SwipeHelper;
import com.dougkeen.bart.controls.Ticker;
import com.dougkeen.bart.controls.YourTrainLayout;
import com.dougkeen.bart.data.DepartureArrayAdapter;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.StationPair;
import com.dougkeen.bart.services.BoardedDepartureService;
import com.dougkeen.bart.services.EtdService;
import com.dougkeen.bart.services.EtdService.EtdServiceBinder;
import com.dougkeen.bart.services.EtdService.EtdServiceListener;
import com.dougkeen.bart.services.EtdService_;
import com.dougkeen.util.Assert;
import com.dougkeen.util.Observer;
import com.dougkeen.util.WakeLocker;

public class ViewDeparturesActivity extends AppCompatActivity implements
        EtdServiceListener {

    private StationPair mStationPair;

    private Departure mSelectedDeparture;

    private DepartureArrayAdapter mDeparturesAdapter;

    private TextView mEmptyView;
    private ProgressBar mProgress;

    private ActionMode mActionMode;

    private EtdService mEtdService;

    private Handler mHandler = new Handler();

    private boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.departures);

        final Intent intent = getIntent();

        final BartRunnerApplication bartRunnerApplication = (BartRunnerApplication) getApplication();

        mEmptyView = (TextView) findViewById(android.R.id.empty);
        mEmptyView.setText(R.string.departure_wait_message);

        mProgress = (ProgressBar) findViewById(android.R.id.progress);

        mDeparturesAdapter = new DepartureArrayAdapter(this);

        setListAdapter(mDeparturesAdapter);
        final ListView listView = getListView();
        listView.setEmptyView(findViewById(android.R.id.empty));
        listView.setOnItemClickListener(mListItemClickListener);
        listView.setOnItemLongClickListener(mListItemLongClickListener);

        mYourTrainSection = (YourTrainLayout) findViewById(R.id.yourTrainSection);
        mYourTrainSection.setOnClickListener(mYourTrainSectionClickListener);
        mSwipeHelper = new SwipeHelper(mYourTrainSection, null,
                new SwipeHelper.OnDismissCallback() {
                    @Override
                    public void onDismiss(View view, Object token) {
                        dismissYourTrainSelection();
                        if (mActionMode != null) {
                            mActionMode.finish();
                        }
                    }
                });
        mYourTrainSection.setOnTouchListener(mSwipeHelper);

        if (savedInstanceState != null
                && savedInstanceState.containsKey("stationPair")) {
            mStationPair = savedInstanceState.getParcelable("stationPair");
            setListTitle();
        } else {
            mStationPair = intent.getExtras().getParcelable(
                    Constants.STATION_PAIR_EXTRA);
            setListTitle();
            if (mBound && mEtdService != null)
                mEtdService
                        .registerListener(ViewDeparturesActivity.this, false);
            refreshBoardedDeparture(false);
        }

        if (savedInstanceState != null) {
            Parcelable[] departuresArray = savedInstanceState.getParcelableArray("departures");
            if (departuresArray != null) {
                for (Parcelable departure : departuresArray) {
                    mDeparturesAdapter.add((Departure) departure);
                }
                mDeparturesAdapter.notifyDataSetChanged();
            }
            if (savedInstanceState.containsKey("selectedDeparture")) {
                setSelectedDeparture((Departure) savedInstanceState
                        .getParcelable("selectedDeparture"));
            }
            if (savedInstanceState.getBoolean("hasDepartureActionMode")
                    && mSelectedDeparture != null) {
                startDepartureActionMode();
            }
            if (savedInstanceState.getBoolean("hasYourTrainActionMode")
                    && mSelectedDeparture != null) {
                ((Checkable) findViewById(R.id.yourTrainSection))
                        .setChecked(true);
                startYourTrainActionMode();
            }
        }
        refreshBoardedDeparture(false);

        ActionBar supportActionBar = Assert.notNull(getSupportActionBar());
        supportActionBar.setHomeButtonEnabled(true);
        supportActionBar.setDisplayHomeAsUpEnabled(true);

        if (bartRunnerApplication.shouldPlayAlarmRingtone()) {
            soundTheAlarm();
        }

        if (bartRunnerApplication.isAlarmSounding()) {
            Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.train_alarm_text)
                    .setCancelable(false)
                    .setNeutralButton(R.string.silence_alarm,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    silenceAlarm();
                                    dialog.dismiss();
                                }
                            }).show();
        }
    }

    private void setSelectedDeparture(Departure departure) {
        if (mSelectedDeparture != null && !mSelectedDeparture.equals(departure)) {
            mSelectedDeparture.setSelected(false);
        }
        if (departure != null) {
            departure.setSelected(true);
        }
        mSelectedDeparture = departure;
        mDeparturesAdapter.notifyDataSetChanged();
    }

    private void soundTheAlarm() {
        final BartRunnerApplication application = (BartRunnerApplication) getApplication();

        Uri alarmSound = RingtoneManager
                .getDefaultUri(RingtoneManager.TYPE_ALARM);

        if (alarmSound == null || !tryToPlayRingtone(alarmSound)) {
            alarmSound = RingtoneManager
                    .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (alarmSound == null || !tryToPlayRingtone(alarmSound)) {
                alarmSound = RingtoneManager
                        .getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }
        if (application.getAlarmMediaPlayer() == null) {
            tryToPlayRingtone(alarmSound);
        }
        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(new long[]{0, 500, 500}, 1);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                silenceAlarm();
            }
        }, 20000);

        application.setPlayAlarmRingtone(false);
        application.setAlarmSounding(true);
    }

    private boolean tryToPlayRingtone(Uri alertSound) {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, alertSound);
        if (mediaPlayer == null)
            return false;
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
        ((BartRunnerApplication) getApplication())
                .setAlarmMediaPlayer(mediaPlayer);
        return true;
    }

    private void silenceAlarm() {
        final BartRunnerApplication application = (BartRunnerApplication) getApplication();
        final MediaPlayer mediaPlayer = application.getAlarmMediaPlayer();
        application.setAlarmSounding(false);
        application.setAlarmMediaPlayer(null);
        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.cancel();
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        } catch (IllegalStateException e) {
            Log.e(Constants.TAG,
                    "Couldn't stop media player; It was in an invalid state", e);
        }
    }

    private void setListTitle() {
        ((TextView) findViewById(R.id.listTitle))
                .setText(mStationPair.getOrigin().name + " to "
                        + mStationPair.getDestination().name);
    }

    private ListView getListView() {
        return (ListView) findViewById(android.R.id.list);
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
            mBound = true;
            if (getStationPair() != null) {
                mEtdService
                        .registerListener(ViewDeparturesActivity.this, false);
            }
        }
    };

    private boolean mWasLongClick = false;

    private final AdapterView.OnItemClickListener mListItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view,
                                int position, long id) {
            if (mWasLongClick) {
                mWasLongClick = false;
                return;
            }

            if (mActionMode != null) {
                /*
                 * If action mode is displayed, cancel out of that
                 */
                mActionMode.finish();
                getListView().clearChoices();
            } else {
                /*
                 * Otherwise select the clicked departure as the one the user
                 * wants to board
                 */
                setBoardedDeparture(
                        getListAdapter().getItem(position), true);
            }
        }
    };

    private final AdapterView.OnItemLongClickListener mListItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view,
                                       int position, long id) {
            mWasLongClick = true;
            setSelectedDeparture(getListAdapter().getItem(position));
            startDepartureActionMode();
            return false;
        }
    };

    private final View.OnClickListener mYourTrainSectionClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ((Checkable) v).setChecked(true);
            startYourTrainActionMode();
        }
    };

    private YourTrainLayout mYourTrainSection;

    private SwipeHelper mSwipeHelper;

    protected DepartureArrayAdapter getListAdapter() {
        return mDeparturesAdapter;
    }

    protected void setListAdapter(DepartureArrayAdapter adapter) {
        mDeparturesAdapter = adapter;
        getListView().setAdapter(mDeparturesAdapter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mEtdService != null)
            mEtdService.unregisterListener(this);
        if (mBound)
            unbindService(mConnection);
        Ticker.getInstance().stopTicking(this);
        WakeLocker.release();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mStationPair != null) {
            /*
             * If origin or destination are null, this thing was never
             * initialized in the first place, so there's really nothing to save
             */
            Departure[] departures = new Departure[mDeparturesAdapter
                    .getCount()];
            for (int i = mDeparturesAdapter.getCount() - 1; i >= 0; i--) {
                departures[i] = mDeparturesAdapter.getItem(i);
            }
            outState.putParcelableArray("departures", departures);
            outState.putParcelable("selectedDeparture", mSelectedDeparture);
            outState.putBoolean("hasDepartureActionMode",
                    isDepartureActionModeActive());
            outState.putBoolean("hasYourTrainActionMode",
                    isYourTrainActionModeActive());
            outState.putParcelable("stationPair", mStationPair);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(EtdService_.intent(this).get(), mConnection,
                Context.BIND_AUTO_CREATE);
        Ticker.getInstance().startTicking(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow()
                    .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (getWindow() != null)
                        getWindow().clearFlags(
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                }
            }, 10 * 60 * 1000);
            Ticker.getInstance().startTicking(this);
            refreshBoardedDeparture(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.route_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            RoutesListActivity_.intent(this)
                    .flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
            return true;
        } else if (itemId == R.id.view_on_bart_site_button) {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://m.bart.gov/schedules/qp_results.aspx?type=departure&date=today&time="
                            + DateFormat.format("h:mmaa",
                            System.currentTimeMillis())
                            + "&orig="
                            + mStationPair.getOrigin().abbreviation
                            + "&dest="
                            + mStationPair.getDestination().abbreviation)));
            return true;
        } else if (itemId == R.id.view_system_map_button) {
            startActivity(new Intent(this, ViewMapActivity.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void refreshBoardedDeparture(boolean animate) {
        final Departure boardedDeparture = getBoardedDeparture();
        int currentVisibility = mYourTrainSection.getVisibility();

        if (!doesDepartureApply(boardedDeparture)) {
            if (currentVisibility != View.GONE) {
                hideYourTrainSection();
            }
            return;
        }

        mYourTrainSection.updateFromBoardedDeparture(boardedDeparture);

        if (currentVisibility != View.VISIBLE) {
            showYourTrainSection(animate);
        }
    }

    private boolean doesDepartureApply(final Departure departure) {
        return departure != null && departure.getStationPair() != null
                && departure.getStationPair().equals(getStationPair());
    }

    private void setBoardedDeparture(Departure selectedDeparture,
                                     boolean startActionMode) {
        final BartRunnerApplication application = (BartRunnerApplication) getApplication();
        selectedDeparture.setPassengerDestination(mStationPair.getDestination());
        application.setBoardedDeparture(selectedDeparture);
        refreshBoardedDeparture(true);

        // Start the notification service
        final Intent intent = new Intent(ViewDeparturesActivity.this,
                BoardedDepartureService.class);
        intent.putExtra("departure", selectedDeparture);
        startService(intent);

        if (startActionMode) {
            mYourTrainSection.setChecked(true);
            startYourTrainActionMode();
        }
    }

    private void startDepartureActionMode() {
        if (mActionMode == null)
            mActionMode = startSupportActionMode(new DepartureActionMode());
        mActionMode.setTitle(mSelectedDeparture.getTrainDestinationName());
        mActionMode.setSubtitle(mSelectedDeparture.getTrainLengthAndPlatform());
    }

    private class DepartureActionMode implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.departure_context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            ((Checkable) findViewById(R.id.yourTrainSection)).setChecked(false);
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.boardTrain) {
                setBoardedDeparture(mSelectedDeparture, false);

                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            setSelectedDeparture(null);
            mActionMode = null;
        }

    }

    private void startYourTrainActionMode() {
        if (mActionMode == null)
            mActionMode = startSupportActionMode(new YourTrainActionMode());
        mActionMode.setTitle(R.string.your_train);
        Departure boardedDeparture = getBoardedDeparture();
        if (boardedDeparture != null && boardedDeparture.isAlarmPending()) {
            int leadTime = boardedDeparture.getAlarmLeadTimeMinutes();
            mActionMode.setSubtitle(getAlarmSubtitle(leadTime));
        } else {
            mActionMode.setSubtitle(null);
        }
    }

    private String getAlarmSubtitle(int leadTime) {
        if (leadTime == 0)
            return null;
        return "Alarm " + leadTime + " minute" + (leadTime != 1 ? "s" : "")
                + " before departure";
    }

    private class YourTrainActionMode implements ActionMode.Callback {
        private Observer<Boolean> mAlarmPendingObserver;
        private Observer<Integer> mAlarmLeadTimeObserver;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater()
                    .inflate(R.menu.your_train_context_menu, menu);
            final MenuItem cancelAlarmButton = menu
                    .findItem(R.id.cancel_alarm_button);
            final MenuItem setAlarmButton = menu
                    .findItem(R.id.set_alarm_button);
            final Departure boardedDeparture = getBoardedDeparture();

            if (boardedDeparture == null) {
                mode.finish();
                refreshBoardedDeparture(true);
                return true;
            }

            if (boardedDeparture.isAlarmPending()) {
                cancelAlarmButton.setVisible(true);
                setAlarmButton.setIcon(R.drawable.ic_action_alarm);
            } else if (boardedDeparture.getMeanSecondsLeft() > 60) {
                setAlarmButton.setIcon(R.drawable.ic_action_add_alarm);
            }

            // Don't allow alarm setting if train is about to leave
            if (boardedDeparture.getMeanSecondsLeft() / 60 < 1) {
                menu.findItem(R.id.set_alarm_button).setVisible(false);
            }

            mAlarmPendingObserver = new Observer<Boolean>() {
                @Override
                public void onUpdate(final Boolean newValue) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cancelAlarmButton.setVisible(newValue);
                            if (newValue) {
                                mActionMode
                                        .setSubtitle(getAlarmSubtitle(boardedDeparture
                                                .getAlarmLeadTimeMinutes()));
                                setAlarmButton
                                        .setIcon(R.drawable.ic_action_alarm);
                            } else {
                                mActionMode.setSubtitle(null);
                                setAlarmButton
                                        .setIcon(R.drawable.ic_action_add_alarm);
                            }
                        }
                    });
                }
            };
            mAlarmLeadTimeObserver = new Observer<Integer>() {
                @Override
                public void onUpdate(final Integer newValue) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mActionMode.setSubtitle(getAlarmSubtitle(newValue));
                        }
                    });
                }
            };
            boardedDeparture.getAlarmPendingObservable().registerObserver(
                    mAlarmPendingObserver);
            boardedDeparture.getAlarmLeadTimeMinutesObservable()
                    .registerObserver(mAlarmLeadTimeObserver);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            getListView().clearChoices();
            getListView().requestLayout();
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            final int itemId = item.getItemId();
            final Departure boardedDeparture = getBoardedDeparture();
            if (itemId == R.id.set_alarm_button) {
                // Don't prompt for alarm if train is about to leave
                if (boardedDeparture.getMeanSecondsLeft() > 60) {
                    new TrainAlarmDialogFragment()
                            .show(getSupportFragmentManager(), TrainAlarmDialogFragment.TAG);
                }

                return true;
            } else if (itemId == R.id.cancel_alarm_button) {
                Intent intent = new Intent(ViewDeparturesActivity.this,
                        BoardedDepartureService.class);
                intent.putExtra("cancelNotifications", true);
                startService(intent);
                return true;
            } else if (itemId == R.id.delete) {
                mSwipeHelper.dismissWithAnimation(true);
                mode.finish();
                return true;
            } else if (itemId == R.id.share_arrival) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, "My BART train");
                intent.putExtra(
                        Intent.EXTRA_TEXT,
                        getString(
                                R.string.arrival_message,
                                boardedDeparture.getStationPair()
                                        .getDestination().name,
                                boardedDeparture
                                        .getEstimatedArrivalTimeText(ViewDeparturesActivity.this, false)));

                startActivity(Intent.createChooser(intent,
                        getString(R.string.share_arrival_time)));
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            ((Checkable) findViewById(R.id.yourTrainSection)).setChecked(false);

            final Departure boardedDeparture = getBoardedDeparture();
            if (boardedDeparture != null) {
                boardedDeparture.getAlarmPendingObservable()
                        .unregisterObserver(mAlarmPendingObserver);
                boardedDeparture.getAlarmLeadTimeMinutesObservable()
                        .unregisterObserver(mAlarmLeadTimeObserver);
            }

            mAlarmPendingObserver = null;
            mAlarmLeadTimeObserver = null;
            mActionMode = null;
        }
    }

    private void dismissYourTrainSelection() {
        Intent intent = new Intent(ViewDeparturesActivity.this,
                BoardedDepartureService.class);
        intent.putExtra("clearBoardedDeparture", true);
        startService(intent);
        hideYourTrainSection();
    }

    @Override
    public void onETDChanged(final List<Departure> departures) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (departures.isEmpty()) {
                    final TextView textView = mEmptyView;
                    textView.setText(R.string.no_data_message);
                    mProgress.setVisibility(View.GONE);
                    Linkify.addLinks(textView, Linkify.WEB_URLS);
                } else {
                    // TODO: Figure out why Ticker occasionally stops
                    Ticker.getInstance().startTicking(
                            ViewDeparturesActivity.this);

                    Departure boardedDeparture = getBoardedDeparture();
                    boolean boardedDepartureFound = false;

                    // Merge lists
                    if (mDeparturesAdapter.getCount() > 0) {
                        int adapterIndex = -1;
                        for (Departure departure : departures) {
                            if (!boardedDepartureFound
                                    && departure.equals(boardedDeparture)) {
                                boardedDepartureFound = true;
                            }

                            adapterIndex++;
                            Departure existingDeparture = null;
                            if (adapterIndex < mDeparturesAdapter.getCount()) {
                                existingDeparture = mDeparturesAdapter
                                        .getItem(adapterIndex);
                            }
                            while (existingDeparture != null
                                    && !departure.equals(existingDeparture)) {
                                mDeparturesAdapter.remove(existingDeparture);
                                if (adapterIndex < mDeparturesAdapter
                                        .getCount()) {
                                    existingDeparture = mDeparturesAdapter
                                            .getItem(adapterIndex);
                                } else {
                                    existingDeparture = null;
                                }
                            }
                            if (existingDeparture != null) {
                                existingDeparture.mergeEstimate(departure);
                            } else {
                                mDeparturesAdapter.add(departure);
                            }
                        }
                    } else {
                        final DepartureArrayAdapter listAdapter = getListAdapter();
                        listAdapter.clear();
                        for (Departure departure : departures) {
                            listAdapter.add(departure);
                        }
                    }

                    if (doesDepartureApply(boardedDeparture)
                            && !boardedDepartureFound) {
                        boardedDeparture.setListedInETDs(false);
                    }

                    refreshBoardedDeparture(true);

                    getListAdapter().notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onError(final String errorMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO(fuegofro) - see if there's a way to not use toasts
                Toast.makeText(ViewDeparturesActivity.this, errorMessage,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRequestStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgress.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onRequestEnded() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgress.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public StationPair getStationPair() {
        return mStationPair;
    }

    private void hideYourTrainSection() {
        mYourTrainSection.setVisibility(View.GONE);
    }

    private void showYourTrainSection(boolean animate) {
        mYourTrainSection.setVisibility(View.VISIBLE);
        if (animate) {
            mSwipeHelper.showWithAnimation();
        }
    }

    private boolean isYourTrainActionModeActive() {
        return mActionMode != null
                && mActionMode.getTitle()
                .equals(getString(R.string.your_train));
    }

    private boolean isDepartureActionModeActive() {
        return mActionMode != null
                && !mActionMode.getTitle().equals(
                getString(R.string.your_train));
    }

    private Departure getBoardedDeparture() {
        return ((BartRunnerApplication) getApplication()).getBoardedDeparture();
    }
}
