package com.dougkeen.bart.model;

import java.util.ArrayList;
import java.util.List;

public class ScheduleInformation {

    public ScheduleInformation(Station origin, Station destination) {
        super();
        this.origin = origin;
        this.destination = destination;
    }

    private Station origin;
    private Station destination;
    private long date;
    private List<ScheduleItem> trips;

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

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public List<ScheduleItem> getTrips() {
        if (trips == null) {
            trips = new ArrayList<ScheduleItem>();
        }
        return trips;
    }

    public void setTrips(List<ScheduleItem> trips) {
        this.trips = trips;
    }

    public void addTrip(ScheduleItem trip) {
        getTrips().add(trip);
    }

    public long getLatestDepartureTime() {
        if (getTrips().isEmpty())
            return -1;
        else
            return getTrips().get(getTrips().size() - 1).getDepartureTime();
    }

    private int aveTripLength = -1;
    private int tripCount = 0;

    public int getAverageTripLength() {
        if (aveTripLength < 0) {
            int sum = 0;
            for (ScheduleItem trip : getTrips()) {
                int tripLength = trip.getTripLength();
                if (tripLength > 0) {
                    sum += tripLength;
                    tripCount++;
                }
            }
            if (tripCount > 0) {
                aveTripLength = sum / tripCount;
            }
        }
        return aveTripLength;
    }

    public int getTripCountForAverage() {
        getAverageTripLength();
        return tripCount;
    }
}