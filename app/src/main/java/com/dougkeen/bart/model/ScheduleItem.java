package com.dougkeen.bart.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScheduleItem {

    public ScheduleItem() {
        super();
    }

    public ScheduleItem(Station origin, Station destination) {
        super();
        this.origin = origin;
        this.destination = destination;
    }

    public static final int SCHEDULE_ITEM_DEPARTURE_EQUALS_TOLERANCE = 120000;

    private Station origin;
    private Station destination;
    private String fare;
    private long departureTime;
    private long arrivalTime;
    private boolean bikesAllowed;
    private String trainHeadStation;

    public Station getOrigin() {
        return origin;
    }

    public void setOrigin(Station origin) {
        this.origin = origin;
    }

    public Station getDestination() {
        return destination;
    }

    public void setDestination(Station destination) {
        this.destination = destination;
    }

    public String getFare() {
        return fare;
    }

    public void setFare(String fare) {
        this.fare = fare;
    }

    public long getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(long departureTime) {
        this.departureTime = departureTime;
    }

    public long getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(long arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getTripLength() {
        if (departureTime <= 0 || arrivalTime <= 0) {
            return 0;
        } else {
            return (int) (arrivalTime - departureTime);
        }
    }

    public boolean isBikesAllowed() {
        return bikesAllowed;
    }

    public void setBikesAllowed(boolean bikesAllowed) {
        this.bikesAllowed = bikesAllowed;
    }

    public String getTrainHeadStation() {
        return trainHeadStation;
    }

    public void setTrainHeadStation(String trainHeadStation) {
        this.trainHeadStation = trainHeadStation;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        DateFormat format = SimpleDateFormat.getTimeInstance();
        builder.append("ScheduleItem [origin=");
        builder.append(origin);
        builder.append(", destination=");
        builder.append(destination);
        builder.append(", fare=");
        builder.append(fare);
        builder.append(", departureTime=");
        builder.append(format.format(new Date(departureTime)));
        builder.append(", arrivalTime=");
        builder.append(format.format(new Date(arrivalTime)));
        builder.append(", bikesAllowed=");
        builder.append(bikesAllowed);
        builder.append(", trainHeadStation=");
        builder.append(trainHeadStation);
        builder.append("]");
        return builder.toString();
    }

}
