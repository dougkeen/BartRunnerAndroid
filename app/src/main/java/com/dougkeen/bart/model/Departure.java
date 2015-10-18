package com.dougkeen.bart.model;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import com.dougkeen.bart.BartRunnerApplication;
import com.dougkeen.bart.R;
import com.dougkeen.bart.activities.ViewDeparturesActivity;
import com.dougkeen.bart.services.BoardedDepartureService;
import com.dougkeen.util.Observable;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Departure implements Parcelable, Comparable<Departure> {
    private static final int MINIMUM_MERGE_OVERLAP_MILLIS = 5000;
    private static final int EXPIRE_MINUTES_AFTER_ARRIVAL = 1;

    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("h:mm");

    public Departure() {
        super();
    }

    public Departure(String destinationAbbr, String destinationColor,
                     String platform, String direction, boolean bikeAllowed,
                     String trainLength, int minutes) {
        super();
        this.trainDestination = Station.getByAbbreviation(destinationAbbr);
        this.destinationColor = destinationColor;
        this.platform = platform;
        this.direction = direction;
        this.bikeAllowed = bikeAllowed;
        this.trainLength = trainLength;
        this.minutes = minutes;
    }

    public Departure(Parcel in) {
        readFromParcel(in);
    }

    private Station origin;
    private Station trainDestination;
    private Station passengerDestination;
    private Line line;
    private String destinationColor;
    private @ColorInt int destinationColorInt;
    private String platform;
    private String direction;
    private boolean bikeAllowed;
    private String trainLength;
    private boolean requiresTransfer;
    private boolean transferScheduled;

    private int minutes;

    private long minEstimate;
    private long maxEstimate;

    private int estimatedTripTime;

    private boolean beganAsDeparted;

    private long arrivalTimeOverride;

    private Observable<Integer> alarmLeadTimeMinutes = new Observable<Integer>(
            0);
    private Observable<Boolean> alarmPending = new Observable<Boolean>(false);

    private boolean listedInETDs = true;

    private boolean selected;

    public Station getOrigin() {
        return origin;
    }

    public void setOrigin(Station origin) {
        this.origin = origin;
    }

    public Station getTrainDestination() {
        return trainDestination;
    }

    public void setTrainDestination(Station destination) {
        this.trainDestination = destination;
    }

    public String getTrainDestinationName() {
        if (trainDestination != null)
            return trainDestination.name;
        return null;
    }

    public String getTrainDestinationAbbreviation() {
        if (trainDestination != null)
            return trainDestination.abbreviation;
        return null;
    }

    public Station getPassengerDestination() {
        return passengerDestination;
    }

    public void setPassengerDestination(Station passengerDestination) {
        this.passengerDestination = passengerDestination;
    }

    public StationPair getStationPair() {
        if (passengerDestination != null) {
            return new StationPair(origin, passengerDestination);
        } else {
            return null;
        }
    }

    public Line getLine() {
        return line;
    }

    public void setLine(Line line) {
        this.line = line;
    }

    @ColorInt
    public int getTrainDestinationColor() {
        if (destinationColorInt == 0) {
            destinationColorInt = Color.parseColor(destinationColor);
        }
        return destinationColorInt;
    }

    public void setTrainDestinationColor(String destinationColor) {
        this.destinationColor = destinationColor;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public boolean isBikeAllowed() {
        return bikeAllowed;
    }

    public void setBikeAllowed(boolean bikeAllowed) {
        this.bikeAllowed = bikeAllowed;
    }

    public String getTrainLength() {
        return trainLength;
    }

    public void setTrainLength(String trainLength) {
        this.trainLength = trainLength;
    }

    public String getTrainLengthText() {
        return trainLength + " cars";
    }

    public String getTrainLengthAndPlatform() {
        return trainLength + " cars, platform " + getPlatform();
    }

    public boolean getRequiresTransfer() {
        return requiresTransfer;
    }

    public void setRequiresTransfer(boolean requiresTransfer) {
        this.requiresTransfer = requiresTransfer;
    }

    public boolean isTransferScheduled() {
        return transferScheduled;
    }

    public void setTransferScheduled(boolean transferScheduled) {
        this.transferScheduled = transferScheduled;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
        if (minutes == 0) {
            beganAsDeparted = true;
        }
    }

    public long getMinEstimate() {
        return minEstimate;
    }

    public void setMinEstimate(long minEstimate) {
        this.minEstimate = minEstimate;
    }

    public long getMaxEstimate() {
        return maxEstimate;
    }

    public void setMaxEstimate(long maxEstimate) {
        this.maxEstimate = maxEstimate;
    }

    public int getEstimatedTripTime() {
        return estimatedTripTime;
    }

    public void setEstimatedTripTime(int estimatedTripTime) {
        this.estimatedTripTime = estimatedTripTime;
    }

    public boolean hasEstimatedTripTime() {
        return this.estimatedTripTime > 0;
    }

    public boolean hasAnyArrivalEstimate() {
        return this.estimatedTripTime > 0 || this.arrivalTimeOverride > 0;
    }

    public int getUncertaintySeconds() {
        return (int) (maxEstimate - minEstimate + 1000) / 2000;
    }

    public int getMinSecondsLeft() {
        return (int) ((getMinEstimate() - System.currentTimeMillis()) / 1000);
    }

    public int getMaxSecondsLeft() {
        return (int) ((getMaxEstimate() - System.currentTimeMillis()) / 1000);
    }

    public int getMeanSecondsLeft() {
        return getMeanSecondsLeft(getMinEstimate(), getMaxEstimate());
    }

    public int getMeanSecondsLeft(long min, long max) {
        return (int) ((getMeanEstimate(min, max) - System.currentTimeMillis()) / 1000);
    }

    public long getMeanEstimate() {
        return getMeanEstimate(getMinEstimate(), getMaxEstimate());
    }

    public long getMeanEstimate(long min, long max) {
        return (min + max) / 2;
    }

    public long getArrivalTimeOverride() {
        return arrivalTimeOverride;
    }

    public void setArrivalTimeOverride(long arrivalTimeOverride) {
        this.arrivalTimeOverride = arrivalTimeOverride;
    }

    public long getEstimatedArrivalTime() {
        if (arrivalTimeOverride > 0) {
            return arrivalTimeOverride;
        }
        return getMeanEstimate() + getEstimatedTripTime();
    }

    public long getEstimatedArrivalMinutesLeft() {
        long millisLeft = getEstimatedArrivalTime()
                - System.currentTimeMillis();
        if (millisLeft < 0) {
            return -1;
        } else {
            // Add ~30s to emulate rounding
            return (millisLeft + 29999) / (60 * 1000);
        }
    }

    public String getEstimatedArrivalMinutesLeftText(Context context) {
        if (!hasAnyArrivalEstimate()) {
            return "Estimated arrival unknown";
        }
        long minutesLeft = getEstimatedArrivalMinutesLeft();
        if (minutesLeft < 0) {
            return "Arrived at destination";
        } else if (minutesLeft == 0) {
            return "Arrives ~" + getEstimatedArrivalTimeText(context, false)
                    + " (<1 min)";
        } else if (minutesLeft == 1) {
            return "Arrives ~" + getEstimatedArrivalTimeText(context, false)
                    + " (1 min)";
        } else {
            return "Arrives ~" + getEstimatedArrivalTimeText(context, false)
                    + " (" + minutesLeft + " mins)";
        }
    }

    public String getEstimatedArrivalTimeText(Context context) {
        return getEstimatedArrivalTimeText(context, false);
    }

    public String getEstimatedArrivalTimeText(Context context, boolean compact) {
        if (getEstimatedTripTime() > 0 || arrivalTimeOverride > 0) {
            final Date arrivalTime = new Date(getEstimatedArrivalTime());
            if (compact) {
                return TIME_FORMAT.format(arrivalTime);
            } else {
                return android.text.format.DateFormat.getTimeFormat(context)
                        .format(arrivalTime);
            }
        } else {
            return "";
        }
    }

    public String getEstimatedDepartureTimeText(Context context) {
        return getEstimatedDepartureTimeText(context, false);
    }

    public String getEstimatedDepartureTimeText(Context context, boolean compact) {
        if (getMeanEstimate() > 0) {
            final Date departureTime = new Date(getMeanEstimate());
            if (compact) {
                return TIME_FORMAT.format(departureTime);
            } else {
                return android.text.format.DateFormat.getTimeFormat(context)
                        .format(departureTime);
            }
        } else {
            return "";
        }
    }

    public boolean hasDeparted() {
        return getMeanSecondsLeft() <= 0;
    }

    public void calculateEstimates(long originalEstimateTime) {
        setMinEstimate(originalEstimateTime + (getMinutes() * 60 * 1000)
                - (30000));
        setMaxEstimate(getMinEstimate() + 60000);
    }

    public void mergeEstimate(Departure departure) {
        if (departure.hasDeparted() && origin.longStationLinger
                && getMinEstimate() > 0 && !beganAsDeparted) {
            /*
             * This is probably not a true departure, but an indication that the
             * train is in the station. Don't update the estimates.
             */
            return;
        }

        boolean wasDeparted = hasDeparted();
        if (!hasAnyArrivalEstimate() && departure.hasAnyArrivalEstimate()) {
            setArrivalTimeOverride(departure.getArrivalTimeOverride());
            setEstimatedTripTime(departure.getEstimatedTripTime());
        }

        long newMin = Math.max(getMinEstimate(), departure.getMinEstimate());
        long newMax = Math.min(getMaxEstimate(), departure.getMaxEstimate());

        if ((getMaxEstimate() - departure.getMinEstimate()) < MINIMUM_MERGE_OVERLAP_MILLIS
                || departure.getMaxEstimate() - getMinEstimate() < MINIMUM_MERGE_OVERLAP_MILLIS) {
            /*
             * The estimate must have changed... just use the latest incoming
             * values
             */
            newMin = departure.getMinEstimate();
            newMax = departure.getMaxEstimate();
        }

        /*
         * If the new departure would mark this as departed, and we have < 60
         * seconds left on a fairly accurate local estimate, ignore the incoming
         * departure
         */
        if (!wasDeparted && getMeanSecondsLeft(newMin, newMax) <= 0
                && getMeanSecondsLeft() < 60 && getUncertaintySeconds() < 30) {
            Log.d(Constants.TAG,
                    "Skipping estimate merge, since it would make this departure show as 'departed' prematurely");
            return;
        }

        if (newMax > newMin) {
            // We must never have 0 or negative uncertainty
            setMinEstimate(newMin);
            setMaxEstimate(newMax);
        }
    }

    public boolean hasExpired() {
        final long now = System.currentTimeMillis();
        return getMaxEstimate() < now
                && getEstimatedArrivalTime() + EXPIRE_MINUTES_AFTER_ARRIVAL
                * 60000 < now;
    }

    public int compareTo(Departure another) {
        return (this.getMeanSecondsLeft() > another.getMeanSecondsLeft()) ? 1
                : ((this.getMeanSecondsLeft() == another.getMeanSecondsLeft()) ? 0
                : -1);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (bikeAllowed ? 1231 : 1237);
        result = prime
                * result
                + ((trainDestination == null) ? 0 : trainDestination.hashCode());
        result = prime
                * result
                + ((destinationColor == null) ? 0 : destinationColor.hashCode());
        result = prime * result
                + ((direction == null) ? 0 : direction.hashCode());
        result = prime * result + ((line == null) ? 0 : line.hashCode());
        result = prime * result + (int) (maxEstimate ^ (maxEstimate >>> 32));
        result = prime * result + (int) (minEstimate ^ (minEstimate >>> 32));
        result = prime * result + minutes;
        result = prime * result
                + ((platform == null) ? 0 : platform.hashCode());
        result = prime * result + (requiresTransfer ? 1231 : 1237);
        result = prime * result
                + ((trainLength == null) ? 0 : trainLength.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Departure other = (Departure) obj;
        if (bikeAllowed != other.bikeAllowed)
            return false;
        if (trainDestination != other.trainDestination)
            return false;
        if (destinationColor == null) {
            if (other.destinationColor != null)
                return false;
        } else if (!destinationColor.equals(other.destinationColor))
            return false;
        if (direction == null) {
            if (other.direction != null)
                return false;
        } else if (!direction.equals(other.direction))
            return false;
        if (line != other.line)
            return false;
        if (Math.abs(maxEstimate - other.maxEstimate) > getEqualsTolerance())
            return false;
        if (platform == null) {
            if (other.platform != null)
                return false;
        } else if (!platform.equals(other.platform))
            return false;
        if (requiresTransfer != other.requiresTransfer)
            return false;
        if (trainLength == null) {
            if (other.trainLength != null)
                return false;
        } else if (!trainLength.equals(other.trainLength))
            return false;
        return true;
    }

    private int getEqualsTolerance() {
        if (origin != null) {
            return origin.departureEqualityTolerance;
        } else {
            return Station.DEFAULT_DEPARTURE_EQUALITY_TOLERANCE;
        }
    }

    public String getCountdownText() {
        StringBuilder builder = new StringBuilder();
        int secondsLeft = getMeanSecondsLeft();
        if (hasDeparted()) {
            if (origin != null && origin.longStationLinger && beganAsDeparted) {
                builder.append("At station");
            } else if (isListedInETDs()) {
                builder.append(BartRunnerApplication.getAppContext().getString(
                        R.string.leaving));
            } else {
                builder.append(BartRunnerApplication.getAppContext().getString(
                        R.string.departed));
            }
        } else {
            builder.append(secondsLeft / 60);
            builder.append("m, ");
            builder.append(secondsLeft % 60);
            builder.append("s");
        }
        return builder.toString();
    }

    public String getUncertaintyText() {
        if (hasDeparted()) {
            return "";
        } else {
            return "(±" + getUncertaintySeconds() + "s)";
        }
    }

    public boolean isListedInETDs() {
        return listedInETDs;
    }

    public void setListedInETDs(boolean listedInETDs) {
        this.listedInETDs = listedInETDs;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getAlarmLeadTimeMinutes() {
        return alarmLeadTimeMinutes.getValue();
    }

    public Observable<Integer> getAlarmLeadTimeMinutesObservable() {
        return alarmLeadTimeMinutes;
    }

    public boolean isAlarmPending() {
        return alarmPending.getValue();
    }

    public Observable<Boolean> getAlarmPendingObservable() {
        return alarmPending;
    }

    private PendingIntent getAlarmIntent(Context context) {
        Intent intent = new Intent(context, ViewDeparturesActivity.class);
        intent.putExtra(Constants.STATION_PAIR_EXTRA, getStationPair());
        intent.setAction(Constants.ACTION_ALARM);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private long getAlarmClockTime() {
        return getMeanEstimate() - alarmLeadTimeMinutes.getValue() * 60 * 1000;
    }

    public int getSecondsUntilAlarm() {
        return getMeanSecondsLeft() - getAlarmLeadTimeMinutes() * 60;
    }

    public void setUpAlarm(int leadTimeMinutes) {
        this.alarmLeadTimeMinutes.setValue(leadTimeMinutes);
        this.alarmPending.setValue(true);
    }

    public void updateAlarm(Context context, AlarmManager alarmManager) {
        if (alarmManager == null)
            return;

        if (isAlarmPending() && getAlarmLeadTimeMinutes() > 0) {
            final PendingIntent alarmIntent = getAlarmIntent(context);
            alarmManager.cancel(alarmIntent);

            long alarmTime = getAlarmClockTime();

            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, alarmIntent);

            if (Log.isLoggable(Constants.TAG, Log.VERBOSE))
                Log.v(Constants.TAG,
                        "Scheduling alarm for "
                                + DateFormatUtils.format(alarmTime, "h:mm:ss"));
        }
    }

    public void cancelAlarm(Context context, AlarmManager alarmManager) {
        alarmManager.cancel(getAlarmIntent(context));
        this.alarmPending.setValue(false);
    }

    private PendingIntent notificationIntent;

    private PendingIntent getNotificationIntent(Context context) {
        if (notificationIntent == null) {
            Intent targetIntent = new Intent(context,
                    ViewDeparturesActivity.class);
            targetIntent.putExtra(Constants.STATION_PAIR_EXTRA,
                    getStationPair());
            targetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            notificationIntent = PendingIntent.getActivity(context, 0,
                    targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return notificationIntent;
    }

    public Notification createNotification(Context context) {
        final int halfMinutes = (getMeanSecondsLeft() + 15) / 30;
        float minutes = halfMinutes / 2f;
        final String minutesText = (minutes < 1) ? "Less than one minute"
                : (String.format("~%.1f minute", minutes) + ((minutes != 1.0) ? "s"
                : ""));

        final Intent cancelAlarmIntent = new Intent(context,
                BoardedDepartureService.class);
        cancelAlarmIntent.putExtra("cancelNotifications", true);
        String title = getOrigin().shortName + " to " + getPassengerDestination().shortName;

        Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(title)
                .setContentIntent(getNotificationIntent(context)).setWhen(0);

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            notificationBuilder
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentText(minutesText + " until departure");

            if (isAlarmPending()) {
                PendingIntent pendingIntent = PendingIntent.getService(
                        context, 0, cancelAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                String subText = "Alarm " + getAlarmLeadTimeMinutes() + " minutes before departure";

                notificationBuilder
                        .addAction(R.drawable.ic_action_cancel_alarm, "Cancel alarm", pendingIntent)
                        .setSubText(subText);
            }
        } else if (isAlarmPending()) {
            String text = minutesText
                    + " to departure (alarm at " + getAlarmLeadTimeMinutes()
                    + " min" + ((getAlarmLeadTimeMinutes() == 1) ? "" : "s")
                    + ")";
            notificationBuilder.setContentText(text);
        } else {
            notificationBuilder.setContentText(minutesText + " until departure");
        }

        return notificationBuilder.build();
    }

    @Override
    public String toString() {
        java.text.DateFormat format = SimpleDateFormat.getTimeInstance();
        StringBuilder builder = new StringBuilder();
        builder.append(trainDestination);
        if (requiresTransfer) {
            builder.append(" (w/ xfer)");
        }
        builder.append(", ");
        builder.append(getCountdownText());
        builder.append(", ");
        builder.append(format.format(new Date(getMeanEstimate())));
        return builder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(origin.abbreviation);
        dest.writeString(trainDestination.abbreviation);
        dest.writeString(passengerDestination == null ? null
                : passengerDestination.abbreviation);
        dest.writeString(destinationColor);
        dest.writeString(platform);
        dest.writeString(direction);
        dest.writeByte((byte) (bikeAllowed ? 1 : 0));
        dest.writeString(trainLength);
        dest.writeByte((byte) (requiresTransfer ? 1 : 0));
        dest.writeInt(minutes);
        dest.writeLong(minEstimate);
        dest.writeLong(maxEstimate);
        dest.writeLong(arrivalTimeOverride);
        dest.writeInt(estimatedTripTime);
        dest.writeInt(line.ordinal());
        dest.writeByte(beganAsDeparted ? (byte) 1 : (byte) 0);
        dest.writeByte(bikeAllowed ? (byte) 1 : (byte) 0);
        dest.writeByte(requiresTransfer ? (byte) 1 : (byte) 0);
        dest.writeByte(transferScheduled ? (byte) 1 : (byte) 0);
    }

    private void readFromParcel(Parcel in) {
        origin = Station.getByAbbreviation(in.readString());
        trainDestination = Station.getByAbbreviation(in.readString());
        passengerDestination = Station.getByAbbreviation(in.readString());
        destinationColor = in.readString();
        platform = in.readString();
        direction = in.readString();
        bikeAllowed = in.readByte() != 0;
        trainLength = in.readString();
        requiresTransfer = in.readByte() != 0;
        minutes = in.readInt();
        minEstimate = in.readLong();
        maxEstimate = in.readLong();
        arrivalTimeOverride = in.readLong();
        estimatedTripTime = in.readInt();
        line = Line.values()[in.readInt()];
        beganAsDeparted = in.readByte() == (byte) 1;
        bikeAllowed = in.readByte() == (byte) 1;
        requiresTransfer = in.readByte() == (byte) 1;
        transferScheduled = in.readByte() == (byte) 1;
    }

    public static final Parcelable.Creator<Departure> CREATOR = new Parcelable.Creator<Departure>() {
        public Departure createFromParcel(Parcel in) {
            return new Departure(in);
        }

        public Departure[] newArray(int size) {
            return new Departure[size];
        }
    };

    public void notifyAlarmHasBeenHandled() {
        this.alarmPending.setValue(false);
    }
}
