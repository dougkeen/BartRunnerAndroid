package com.dougkeen.bart.activities;

import java.util.Calendar;
import java.util.TimeZone;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.R;
import com.dougkeen.bart.controls.Ticker;
import com.dougkeen.bart.controls.Ticker.TickSubscriber;
import com.dougkeen.bart.data.FavoritesArrayAdapter;
import com.dougkeen.bart.model.Alert;
import com.dougkeen.bart.model.Alert.AlertList;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.StationPair;
import com.dougkeen.bart.networktasks.AlertsClient;
import com.dougkeen.bart.networktasks.ElevatorClient;
import com.dougkeen.bart.networktasks.GetRouteFareTask;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.App;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.InstanceState;
import com.googlecode.androidannotations.annotations.ItemClick;
import com.googlecode.androidannotations.annotations.ItemLongClick;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.googlecode.androidannotations.annotations.rest.RestService;
import com.mobeta.android.dslv.DragSortListView;

@EActivity(R.layout.main)
public class RoutesListActivity extends AppCompatActivity implements TickSubscriber {
    private static final String NO_DELAYS_REPORTED = "No delays reported";

    private static final TimeZone PACIFIC_TIME = TimeZone
            .getTimeZone("America/Los_Angeles");

    private static final String TAG = "RoutesListActivity";

    @InstanceState
    StationPair mCurrentlySelectedStationPair;

    @InstanceState
    String mCurrentAlerts;

    private ActionMode mActionMode;

    private FavoritesArrayAdapter mRoutesAdapter;

    @App
    BartRunnerApplication app;

    @RestService
    AlertsClient alertsClient;

    @RestService
    ElevatorClient elevatorClient;

    @ViewById(android.R.id.list)
    DragSortListView listView;

    @ViewById(R.id.quickLookupButton)
    Button quickLookupButton;

    @ViewById(R.id.alertMessages)
    TextView alertMessages;

    @Click(R.id.quickLookupButton)
    void quickLookupButtonClick() {
        DialogFragment dialog = new QuickRouteDialogFragment();
        dialog.show(getSupportFragmentManager(), QuickRouteDialogFragment.TAG);
    }

    @ItemClick(android.R.id.list)
    void listItemClicked(StationPair item) {
        Intent intent = new Intent(RoutesListActivity.this,
                ViewDeparturesActivity.class);
        intent.putExtra(Constants.STATION_PAIR_EXTRA, item);
        startActivity(intent);
    }

    @ItemLongClick(android.R.id.list)
    void listItemLongClick(StationPair item) {
        if (mActionMode != null) {
            mActionMode.finish();
        }

        mCurrentlySelectedStationPair = item;

        startContextualActionMode();
    }

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from == to)
                return;

            StationPair item = mRoutesAdapter.getItem(from);

            mRoutesAdapter.move(item, to);
            mRoutesAdapter.notifyDataSetChanged();
        }
    };

    private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
        @Override
        public void remove(int which) {
            mRoutesAdapter.remove(mRoutesAdapter.getItem(which));
            mRoutesAdapter.notifyDataSetChanged();
        }
    };

    @AfterViews
    void afterViews() {
        setTitle(R.string.favorite_routes);

        mRoutesAdapter = new FavoritesArrayAdapter(this,
                R.layout.favorite_listing, app.getFavorites());

        setListAdapter(mRoutesAdapter);

        listView.setEmptyView(findViewById(android.R.id.empty));

        listView.setDropListener(onDrop);
        listView.setRemoveListener(onRemove);

        if (mCurrentAlerts != null) {
            showAlertMessage(mCurrentAlerts);
        }

        startEtdListeners();
        refreshFares();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("hasActionMode")) {
                startContextualActionMode();
            }
        }

        Ticker.getInstance().addSubscriber(this, getApplicationContext());
    }

    private AdapterView<ListAdapter> getListView() {
        return listView;
    }

    protected FavoritesArrayAdapter getListAdapter() {
        return mRoutesAdapter;
    }

    protected void setListAdapter(FavoritesArrayAdapter adapter) {
        mRoutesAdapter = adapter;
        getListView().setAdapter(mRoutesAdapter);
    }

    void addFavorite(StationPair pair) {
        mRoutesAdapter.add(pair);
    }

    private void refreshFares() {
        for (int i = getListAdapter().getCount() - 1; i >= 0; i--) {
            final StationPair stationPair = getListAdapter().getItem(i);

            Calendar now = Calendar.getInstance();
            Calendar lastUpdate = Calendar.getInstance();
            lastUpdate.setTimeInMillis(stationPair.getFareLastUpdated());

            now.setTimeZone(PACIFIC_TIME);
            lastUpdate.setTimeZone(PACIFIC_TIME);

            // Update every day
            if (now.get(Calendar.DAY_OF_YEAR) != lastUpdate
                    .get(Calendar.DAY_OF_YEAR)
                    || now.get(Calendar.YEAR) != lastUpdate.get(Calendar.YEAR)) {
                GetRouteFareTask fareTask = new GetRouteFareTask() {
                    @Override
                    public void onResult(String fare) {
                        stationPair.setFare(fare);
                        stationPair.setFareLastUpdated(System
                                .currentTimeMillis());
                        getListAdapter().notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Exception exception) {
                        // Ignore... we can do this later
                    }
                };
                fareTask.execute(new GetRouteFareTask.Params(stationPair
                        .getOrigin(), stationPair.getDestination()));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("hasActionMode", mActionMode != null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Ticker.getInstance().startTicking(this);
        startEtdListeners();
    }

    private void startEtdListeners() {
        if (mRoutesAdapter != null && !mRoutesAdapter.isEmpty()
                && !mRoutesAdapter.areEtdListenersActive()) {
            mRoutesAdapter.setUpEtdListeners();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRoutesAdapter != null && mRoutesAdapter.areEtdListenersActive()) {
            mRoutesAdapter.clearEtdListeners();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Ticker.getInstance().stopTicking(this);
        app.saveFavorites();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRoutesAdapter != null) {
            mRoutesAdapter.close();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            Ticker.getInstance().startTicking(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.routes_list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private MenuItem elevatorMenuItem;
    private View origElevatorActionView;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.add_favorite_menu_button) {
            new AddRouteDialogFragment().show(getSupportFragmentManager(),
                    AddRouteDialogFragment.TAG);
            return true;
        } else if (itemId == R.id.view_system_map_button) {
            startActivity(new Intent(this, ViewMapActivity.class));
            return true;
        } else if (itemId == R.id.elevator_button) {
            elevatorMenuItem = item;
            fetchElevatorInfo();
            origElevatorActionView = MenuItemCompat.getActionView(elevatorMenuItem);
            MenuItemCompat.setActionView(elevatorMenuItem, R.layout.progress_spinner);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Background
    void fetchAlerts() {
        Log.d(TAG, "Fetching alerts");
        AlertList alertList = alertsClient.getAlerts();
        if (alertList.hasAlerts()) {
            StringBuilder alertText = new StringBuilder();
            boolean firstAlert = true;
            for (Alert alert : alertList.getAlerts()) {
                if (!firstAlert) {
                    alertText.append("\n\n");
                }
                alertText.append(alert.getPostedTime()).append("\n");
                alertText.append(alert.getDescription());
                firstAlert = false;
            }
            showAlertMessage(alertText.toString());
        } else if (alertList.areNoDelaysReported()) {
            showAlertMessage(NO_DELAYS_REPORTED);
        } else {
            hideAlertMessage();
        }
    }

    @UiThread
    void hideAlertMessage() {
        mCurrentAlerts = null;
        alertMessages.setVisibility(View.GONE);
    }

    @UiThread
    void showAlertMessage(String messageText) {
        if (messageText == null) {
            hideAlertMessage();
            return;
        } else if (messageText.equals(NO_DELAYS_REPORTED)) {
            alertMessages.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_allgood, 0, 0, 0);
        } else {
            alertMessages.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_warn, 0, 0, 0);
        }
        mCurrentAlerts = messageText;
        alertMessages.setText(messageText);
        alertMessages.setVisibility(View.VISIBLE);
    }

    @Background
    void fetchElevatorInfo() {
        String elevatorMessage = elevatorClient.getElevatorMessage();
        if (elevatorMessage != null) {
            showElevatorMessage(elevatorMessage);
        }
        resetElevatorMenuGraphic();
    }

    @UiThread
    void resetElevatorMenuGraphic() {
        ActivityCompat.invalidateOptionsMenu(this);
        MenuItemCompat.setActionView(elevatorMenuItem, origElevatorActionView);
    }

    @UiThread
    void showElevatorMessage(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setTitle("Elevator status");
        builder.show();
    }

    private void startContextualActionMode() {
        mActionMode = startSupportActionMode(new RouteActionMode());
        mActionMode.setTitle(mCurrentlySelectedStationPair.getOrigin().name);
        mActionMode.setSubtitle("to "
                + mCurrentlySelectedStationPair.getDestination().name);
    }

    private final class RouteActionMode implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.route_context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.view) {
                Intent intent = new Intent(RoutesListActivity.this,
                        ViewDeparturesActivity.class);
                intent.putExtra(Constants.STATION_PAIR_EXTRA,
                        mCurrentlySelectedStationPair);
                startActivity(intent);
                mode.finish();
                return true;
            } else if (item.getItemId() == R.id.delete) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(
                        RoutesListActivity.this);
                builder.setCancelable(false);
                builder.setMessage("Are you sure you want to delete this route?");
                builder.setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                getListAdapter().remove(
                                        mCurrentlySelectedStationPair);
                                mCurrentlySelectedStationPair = null;
                                mActionMode.finish();
                                dialog.dismiss();
                            }
                        });
                builder.setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.cancel();
                            }
                        });
                builder.show();
                return false;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }

    }

    @Override
    public int getTickInterval() {
        return 90;
    }

    @Override
    public void onTick(long mTickCount) {
        fetchAlerts();
    }
}
