package com.dougkeen.bart.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.R;
import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.RealTimeDepartures;
import com.dougkeen.bart.model.ScheduleInformation;
import com.dougkeen.bart.model.ScheduleItem;
import com.dougkeen.bart.model.Station;
import com.dougkeen.bart.model.StationPair;
import com.dougkeen.bart.networktasks.GetRealTimeDeparturesTask;
import com.dougkeen.bart.networktasks.GetScheduleInformationTask;
import com.googlecode.androidannotations.annotations.EService;

@EService
public class EtdService extends Service {

    private IBinder mBinder;

    private Map<StationPair, EtdServiceEngine> mServiceEngineMap;

    public EtdService() {
        super();
        mBinder = new EtdServiceBinder();
        mServiceEngineMap = new HashMap<StationPair, EtdServiceEngine>();
    }

    public void registerListener(EtdServiceListener listener,
                                 boolean limitToFirstNonDeparted) {
        StationPair stationPair = getStationPairFromListener(listener);
        if (stationPair == null)
            return;

        if (!mServiceEngineMap.containsKey(stationPair)) {
            mServiceEngineMap.put(stationPair,
                    new EtdServiceEngine(stationPair));
        }
        mServiceEngineMap.get(stationPair).registerListener(listener,
                limitToFirstNonDeparted);
    }

    private StationPair getStationPairFromListener(EtdServiceListener listener) {
        StationPair route = listener.getStationPair();
        if (route == null) {
            Log.wtf(Constants.TAG,
                    "Somehow we got a listener that's returning a null route O_o");
        }
        return route;
    }

    public void unregisterListener(EtdServiceListener listener) {
        StationPair stationPair = getStationPairFromListener(listener);
        if (stationPair == null) {
            for (EtdServiceEngine engine : mServiceEngineMap.values()) {
                engine.unregisterListener(listener);
            }
        } else if (mServiceEngineMap.containsKey(stationPair)) {
            mServiceEngineMap.get(stationPair).unregisterListener(listener);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public interface EtdServiceListener {
        void onETDChanged(final List<Departure> departures);

        void onError(String errorMessage);

        void onRequestStarted();

        void onRequestEnded();

        StationPair getStationPair();
    }

    public class EtdServiceBinder extends Binder {
        public EtdService getService() {
            return EtdService.this;
        }
    }

    private class EtdServiceEngine {
        private static final int UNCERTAINTY_THRESHOLD = 17;

        private final StationPair mStationPair;

        private boolean mIgnoreDepartureDirection = false;

        private boolean mPendingEtdRequest = false;

        private Map<EtdServiceListener, Boolean> mListeners;

        private boolean mLimitToFirstNonDeparted = true;

        private List<Departure> mLatestDepartures;
        private ScheduleInformation mLatestScheduleInfo;

        private AsyncTask<StationPair, Integer, RealTimeDepartures> mGetDeparturesTask;
        private AsyncTask<StationPair, Integer, ScheduleInformation> mGetScheduleInformationTask;

        private Handler mRunnableQueue;

        private boolean mStarted = false;

        public EtdServiceEngine(final StationPair route) {
            mStationPair = route;
            mListeners = new HashMap<EtdService.EtdServiceListener, Boolean>();
            mRunnableQueue = new Handler();
            mLatestDepartures = new ArrayList<Departure>();
        }

        protected void registerListener(EtdServiceListener listener,
                                        boolean limitToFirstNonDeparted) {
            mListeners.put(listener, true);
            if (!limitToFirstNonDeparted) {
                mLimitToFirstNonDeparted = false;
            }
            if (!mPendingEtdRequest) {
                mStarted = true;
                fetchLatestDepartures();
            }
        }

        protected void unregisterListener(EtdServiceListener listener) {
            mListeners.remove(listener);
            if (mListeners.isEmpty()) {
                if (mGetDeparturesTask != null
                        && mGetDeparturesTask.getStatus().equals(
                        AsyncTask.Status.RUNNING)) {
                    mGetDeparturesTask.cancel(true);
                }
                if (mGetScheduleInformationTask != null
                        && mGetScheduleInformationTask.getStatus().equals(
                        AsyncTask.Status.RUNNING)) {
                    mGetScheduleInformationTask.cancel(true);
                }
                mStarted = false;
            }
        }

        private void notifyListenersOfETDChange() {
            for (EtdServiceListener listener : mListeners.keySet()) {
                listener.onETDChanged(mLatestDepartures);
            }
        }

        private void notifyListenersOfError(String errorMessage) {
            for (EtdServiceListener listener : mListeners.keySet()) {
                listener.onError(errorMessage);
            }
        }

        private void notifyListenersOfRequestStart() {
            for (EtdServiceListener listener : mListeners.keySet()) {
                listener.onRequestStarted();
            }
        }

        private void notifyListenersOfRequestEnd() {
            for (EtdServiceListener listener : mListeners.keySet()) {
                listener.onRequestEnded();
            }
        }

        private void fetchLatestDepartures() {
            if (mGetDeparturesTask != null
                    && mGetDeparturesTask.equals(AsyncTask.Status.RUNNING)) {
                // Don't overlap fetches
                return;
            }
            if (!mStarted)
                return;

            GetRealTimeDeparturesTask task = new GetRealTimeDeparturesTask(
                    mIgnoreDepartureDirection) {
                @Override
                public void onResult(RealTimeDepartures result) {
                    Log.v(Constants.TAG, "Processing data from server");
                    processLatestDepartures(result);
                    Log.v(Constants.TAG, "Done processing data from server");
                    notifyListenersOfRequestEnd();
                    mPendingEtdRequest = false;
                }

                @Override
                public void onError(Exception e) {
                    Log.w(Constants.TAG, e.getMessage(), e);
                    notifyListenersOfError(getString(R.string.could_not_connect));
                    // Try again in 60s
                    scheduleDepartureFetch(60000);
                    notifyListenersOfRequestEnd();
                }
            };
            mGetDeparturesTask = task;
            Log.v(Constants.TAG, "Fetching data from server");
            task.execute(mStationPair);
            notifyListenersOfRequestStart();
        }

        private void fetchLatestSchedule() {
            if (mGetScheduleInformationTask != null
                    && mGetScheduleInformationTask.getStatus().equals(
                    AsyncTask.Status.RUNNING)) {
                // Don't overlap fetches
                return;
            }

            GetScheduleInformationTask task = new GetScheduleInformationTask() {
                @Override
                public void onResult(ScheduleInformation result) {
                    Log.v(Constants.TAG, "Processing data from server");
                    mLatestScheduleInfo = result;
                    applyScheduleInformation(result);
                    Log.v(Constants.TAG, "Done processing data from server");
                }

                @Override
                public void onError(Exception e) {
                    Log.w(Constants.TAG, e.getMessage(), e);
                    notifyListenersOfError(getString(R.string.could_not_connect));

                    // Try again in 60s
                    scheduleScheduleInfoFetch(60000);
                }
            };
            Log.i(Constants.TAG, "Fetching data from server");
            mGetScheduleInformationTask = task;
            task.execute(mStationPair);
        }

        protected void applyScheduleInformation(ScheduleInformation result) {
            int localAverageLength = mLatestScheduleInfo.getAverageTripLength();

            int departuresCount = mLatestDepartures.size();

            // Let's get smallest interval between departures
            int smallestDepartureInterval = 0;
            long previousDepartureTime = 0;
            for (int departureIndex = 0; departureIndex < departuresCount; departureIndex++) {
                Departure departure = mLatestDepartures.get(departureIndex);
                if (previousDepartureTime == 0) {
                    previousDepartureTime = departure.getMeanEstimate();
                } else if (smallestDepartureInterval == 0) {
                    smallestDepartureInterval = (int) (departure
                            .getMeanEstimate() - previousDepartureTime);
                } else {
                    smallestDepartureInterval = Math
                            .min(smallestDepartureInterval,
                                    (int) (departure.getMeanEstimate() - previousDepartureTime));
                }
            }

            // Match scheduled departures with real time departures in adapter
            int lastSearchIndex = 0;
            int tripCount = mLatestScheduleInfo.getTrips().size();
            boolean departureUpdated = false;
            Departure lastUnestimatedTransfer = null;
            int departuresWithoutEstimates = 0;
            for (int departureIndex = 0; departureIndex < departuresCount; departureIndex++) {
                Departure departure = mLatestDepartures.get(departureIndex);
                for (int i = lastSearchIndex; i < tripCount; i++) {
                    ScheduleItem trip = mLatestScheduleInfo.getTrips().get(i);
                    // Definitely not a match if they have different
                    // destinations
                    if (!departure.getTrainDestination().abbreviation
                            .equals(trip.getTrainHeadStation())) {
                        continue;
                    }

                    long departTimeDiff = Math.abs(trip.getDepartureTime()
                            - departure.getMeanEstimate());
                    final long millisUntilTripDeparture = trip
                            .getDepartureTime() - System.currentTimeMillis();
                    final int equalityTolerance = (departure.getOrigin() != null) ? NumberUtils
                            .max(departure.getOrigin().departureEqualityTolerance,
                                    ScheduleItem.SCHEDULE_ITEM_DEPARTURE_EQUALS_TOLERANCE,
                                    smallestDepartureInterval)
                            : ScheduleItem.SCHEDULE_ITEM_DEPARTURE_EQUALS_TOLERANCE;
                    if (departure.getOrigin() != null
                            && departure.getOrigin().longStationLinger
                            && departure.hasDeparted()
                            && millisUntilTripDeparture > 0
                            && millisUntilTripDeparture < equalityTolerance) {
                        departure.setArrivalTimeOverride(trip.getArrivalTime());
                        lastSearchIndex = i;
                        departureUpdated = true;
                        if (lastUnestimatedTransfer != null) {
                            lastUnestimatedTransfer.setArrivalTimeOverride(trip
                                    .getArrivalTime());
                            departuresWithoutEstimates--;
                        }
                        break;
                    } else if (departTimeDiff <= (equalityTolerance + departure
                            .getUncertaintySeconds() * 1000)
                            && departure.getEstimatedTripTime() != trip
                            .getTripLength()
                            && !(departure.getOrigin().longStationLinger && departure
                            .hasDeparted())) {
                        departure.setEstimatedTripTime(trip.getTripLength());
                        lastSearchIndex = i;
                        departureUpdated = true;
                        if (lastUnestimatedTransfer != null) {
                            lastUnestimatedTransfer.setArrivalTimeOverride(trip
                                    .getArrivalTime());
                            departuresWithoutEstimates--;
                        }
                        break;
                    }
                }

                // Don't estimate for non-scheduled transfers
                if (!departure.getRequiresTransfer()) {
                    if (!departure.hasEstimatedTripTime()
                            && localAverageLength > 0) {
                        // Use the average we just calculated if available
                        departure.setEstimatedTripTime(localAverageLength);
                    } else if (!departure.hasEstimatedTripTime()) {
                        // Otherwise just assume the global average
                        departure.setEstimatedTripTime(mStationPair
                                .getAverageTripLength());
                    }
                } else if (departure.getRequiresTransfer()
                        && !departure.hasAnyArrivalEstimate()) {
                    lastUnestimatedTransfer = departure;
                }

                if (!departure.hasAnyArrivalEstimate()) {
                    departuresWithoutEstimates++;
                }
            }

            if (departureUpdated) {
                notifyListenersOfETDChange();
            }

            // Update global average
            if (mLatestScheduleInfo.getTripCountForAverage() > 0) {
                int newAverageSampleCount = mStationPair
                        .getAverageTripSampleCount()
                        + mLatestScheduleInfo.getTripCountForAverage();
                int newAverage = (mStationPair.getAverageTripLength()
                        * mStationPair.getAverageTripSampleCount() + localAverageLength
                        * mLatestScheduleInfo.getTripCountForAverage())
                        / newAverageSampleCount;

                mStationPair.setAverageTripLength(newAverage);
                mStationPair.setAverageTripSampleCount(newAverageSampleCount);
            }

            /*
             * If we still have some departures without estimates, try again
             * later
             */
            if (departuresWithoutEstimates > 0) {
                scheduleScheduleInfoFetch(20000);
            }
        }

        private void processLatestDepartures(RealTimeDepartures result) {
            if (result.getDepartures().isEmpty()) {
                result.includeTransferRoutes();
            }
            if (result.getDepartures().isEmpty()) {
                result.includeDoubleTransferRoutes();
            }
            if (result.getDepartures().isEmpty()
                    && mStationPair.isBetweenStations(Station.MLBR,
                    Station.SFIA)) {
                /*
                 * Let's try again, ignoring direction (this sometimes comes up
                 * when you travel between Millbrae and SFO... sometimes you
                 * need to travel north and transfer, sometimes you can travel
                 * south for a direct line)
                 */
                mIgnoreDepartureDirection = true;
                scheduleDepartureFetch(50);
                return;
            }

            boolean needsBetterAccuracy = false;

            final Departure boardedDeparture = ((BartRunnerApplication) getApplication())
                    .getBoardedDeparture();

            /*
             * Keep track of first departure, since we'll request another quick
             * refresh if it has departed.
             */
            Departure firstDeparture = null;

            final List<Departure> departures = result.getDepartures();
            if (mLatestDepartures.isEmpty()) {
                // Just copy everything to the departure list
                for (Departure departure : departures) {
                    if (firstDeparture == null) {
                        firstDeparture = departure;
                    }
                    mLatestDepartures.add(departure);
                    if (departure.equals(boardedDeparture)) {
                        boardedDeparture.mergeEstimate(departure);
                    }
                    if (!departure.hasDeparted() && mLimitToFirstNonDeparted) {
                        break;
                    }
                }

                /*
                 * Since all the departures are new, we'll definitely need
                 * better accuracy
                 */
                needsBetterAccuracy = true;
            } else {
                /*
                 * Let's merge the latest departure list with the instance
                 * departure list
                 */
                int instanceListIndex = -1;
                for (Departure departure : departures) {
                    instanceListIndex++;
                    Departure existingDeparture = null;
                    if (instanceListIndex < mLatestDepartures.size()) {
                        existingDeparture = mLatestDepartures
                                .get(instanceListIndex);
                    }
                    /*
                     * Looks for departures at the beginning of the adapter that
                     * aren't in the latest list of departures
                     */
                    while (existingDeparture != null
                            && !departure.equals(existingDeparture)) {
                        // Remove old departure
                        mLatestDepartures.remove(existingDeparture);
                        if (instanceListIndex < mLatestDepartures.size()) {
                            /*
                             * Try again with next departure (keep in mind the
                             * next departure is now at the current index, since
                             * we removed a member)
                             */
                            existingDeparture = mLatestDepartures
                                    .get(instanceListIndex);
                        } else {
                            // Reached the end of the list... give up
                            existingDeparture = null;
                        }
                    }
                    /*
                     * Merge the estimate if we found a matching departure,
                     * otherwise add a new one to the adapter
                     */
                    if (existingDeparture != null) {
                        existingDeparture.mergeEstimate(departure);
                    } else {
                        mLatestDepartures.add(departure);
                        existingDeparture = departure;
                    }

                    // Set first departure
                    if (firstDeparture == null) {
                        firstDeparture = existingDeparture;
                    }

                    // Check if estimate is accurate enough
                    if (existingDeparture.getUncertaintySeconds() > UNCERTAINTY_THRESHOLD) {
                        needsBetterAccuracy = true;
                    }

                    if (departure.equals(boardedDeparture)) {
                        boardedDeparture.mergeEstimate(departure);
                    }

                    if (!departure.hasDeparted() && mLimitToFirstNonDeparted) {
                        break;
                    }
                }
            }
            Collections.sort(mLatestDepartures);
            notifyListenersOfETDChange();
            requestScheduleIfNecessary();

            if (firstDeparture != null) {
                if (needsBetterAccuracy || firstDeparture.hasDeparted()) {
                    // Get more data in 20s
                    scheduleDepartureFetch(20000);
                } else {
                    /*
                     * Get more 90 seconds before next train arrives, right when
                     * next train arrives, or 3 minutes, whichever is sooner
                     */
                    final int intervalUntilNextDeparture = firstDeparture
                            .getMinSecondsLeft() * 1000;
                    final int alternativeInterval = 3 * 60 * 1000;

                    int interval = intervalUntilNextDeparture;
                    if (intervalUntilNextDeparture > 95000
                            && intervalUntilNextDeparture < alternativeInterval) {
                        interval = interval - 90 * 1000;
                    } else if (intervalUntilNextDeparture > alternativeInterval) {
                        interval = alternativeInterval;
                    }

                    if (interval < 0) {
                        interval = 20000;
                    }

                    scheduleDepartureFetch(interval);
                }
            }
        }

        private void requestScheduleIfNecessary() {
            // Bail if there's nothing to match schedules to
            if (mLatestDepartures.isEmpty()) {
                return;
            }

            // Fetch if we don't have anything at all
            if (mLatestScheduleInfo == null) {
                fetchLatestSchedule();
                return;
            }

            /*
             * Otherwise, check if the latest departure doesn't have schedule
             * info... if not, fetch
             */
            Departure lastDeparture = mLatestDepartures.get(mLatestDepartures
                    .size() - 1);
            if (mLatestScheduleInfo.getLatestDepartureTime() < lastDeparture
                    .getMeanEstimate()) {
                fetchLatestSchedule();
                return;
            } else if (!lastDeparture.hasAnyArrivalEstimate()) {
                applyScheduleInformation(mLatestScheduleInfo);
            }
        }

        private long mNextFetchClockTime = 0;

        private void scheduleDepartureFetch(int millisUntilExecute) {
            mPendingEtdRequest = true;
            long now = System.currentTimeMillis();
            long requestedFetchTime = now + millisUntilExecute;
            if (mNextFetchClockTime > now
                    && mNextFetchClockTime < requestedFetchTime) {
                Log.d(Constants.TAG,
                        "Did not schedule departure fetch, since one is already scheduled");
            } else {
                mRunnableQueue.postDelayed(new Runnable() {
                    public void run() {
                        fetchLatestDepartures();
                    }
                }, millisUntilExecute);
                mNextFetchClockTime = requestedFetchTime;
                Log.i(Constants.TAG, "Scheduled another departure fetch in "
                        + millisUntilExecute / 1000 + "s");
            }
        }

        private void scheduleScheduleInfoFetch(int millisUntilExecute) {
            mRunnableQueue.postDelayed(new Runnable() {
                public void run() {
                    fetchLatestSchedule();
                }
            }, millisUntilExecute);
            Log.i(Constants.TAG, "Scheduled another schedule fetch in "
                    + millisUntilExecute / 1000 + "s");
        }

    }
}
