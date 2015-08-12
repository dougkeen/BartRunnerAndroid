package com.dougkeen.bart.model;

import org.apache.commons.lang3.ObjectUtils;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.dougkeen.bart.data.CursorUtils;
import com.dougkeen.bart.data.RoutesColumns;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StationPair implements Parcelable {
    @JsonCreator
    public StationPair(@JsonProperty("origin") Station origin,
                       @JsonProperty("destination") Station destination) {
        super();
        this.origin = origin;
        this.destination = destination;
    }

    public StationPair(Parcel in) {
        readFromParcel(in);
    }

    public static StationPair createFromCursor(Cursor cursor) {
        StationPair pair = new StationPair(
                Station.getByAbbreviation(CursorUtils.getString(cursor,
                        RoutesColumns.FROM_STATION)),
                Station.getByAbbreviation(CursorUtils.getString(cursor,
                        RoutesColumns.TO_STATION)));
        pair.fare = CursorUtils.getString(cursor, RoutesColumns.FARE);
        pair.fareLastUpdated = CursorUtils.getLong(cursor,
                RoutesColumns.FARE_LAST_UPDATED);
        pair.averageTripLength = CursorUtils.getInteger(cursor,
                RoutesColumns.AVERAGE_TRIP_LENGTH);
        pair.averageTripSampleCount = CursorUtils.getInteger(cursor,
                RoutesColumns.AVERAGE_TRIP_SAMPLE_COUNT);
        return pair;
    }

    private Station origin;
    private Station destination;
    private String fare;

    private long fareLastUpdated;
    private int averageTripLength;
    private int averageTripSampleCount;

    public Station getOrigin() {
        return origin;
    }

    public Station getDestination() {
        return destination;
    }

    public String getFare() {
        return fare;
    }

    public void setFare(String fare) {
        this.fare = fare;
    }

    public long getFareLastUpdated() {
        return fareLastUpdated;
    }

    public void setFareLastUpdated(long fareLastUpdated) {
        this.fareLastUpdated = fareLastUpdated;
    }

    public int getAverageTripLength() {
        return averageTripLength;
    }

    public void setAverageTripLength(int averageTripLength) {
        this.averageTripLength = averageTripLength;
    }

    public int getAverageTripSampleCount() {
        return averageTripSampleCount;
    }

    public void setAverageTripSampleCount(int averageTripSampleCount) {
        this.averageTripSampleCount = averageTripSampleCount;
    }

    public boolean isBetweenStations(Station station1, Station station2) {
        return (origin.equals(station1) && destination.equals(station2))
                || (origin.equals(station2) && destination.equals(station1));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((destination == null) ? 0 : destination.hashCode());
        result = prime * result + ((origin == null) ? 0 : origin.hashCode());
        return result;
    }

    public boolean fareEquals(StationPair other) {
        if (other == null)
            return false;
        return ObjectUtils.equals(getFare(), other.getFare())
                && ObjectUtils.equals(getFareLastUpdated(),
                other.getFareLastUpdated());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StationPair other = (StationPair) obj;
        if (destination != other.destination)
            return false;
        return origin == other.origin;
    }

    @Override
    public String toString() {
        return "StationPair [origin=" + origin + ", destination=" + destination
                + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(origin.abbreviation);
        dest.writeString(destination.abbreviation);
    }

    private void readFromParcel(Parcel in) {
        origin = Station.getByAbbreviation(in.readString());
        destination = Station.getByAbbreviation(in.readString());
    }

    public static final Parcelable.Creator<StationPair> CREATOR = new Parcelable.Creator<StationPair>() {
        public StationPair createFromParcel(Parcel in) {
            return new StationPair(in);
        }

        public StationPair[] newArray(int size) {
            return new StationPair[size];
        }
    };
}