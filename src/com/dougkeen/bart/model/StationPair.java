package com.dougkeen.bart.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class StationPair implements Parcelable {
	public StationPair(Station origin, Station destination) {
		super();
		this.origin = origin;
		this.destination = destination;
	}

	public StationPair(Parcel in) {
		readFromParcel(in);
	}

	private Station origin;
	private Station destination;

	public Station getOrigin() {
		return origin;
	}

	public Station getDestination() {
		return destination;
	}

	public boolean isBetweenStations(Station station1, Station station2) {
		return (origin.equals(station1) && destination.equals(station2))
				|| (origin.equals(station2) && destination.equals(station1));
	}

	public Uri getUri() {
		if (getOrigin() != null && getDestination() != null) {
			return Constants.ARBITRARY_ROUTE_CONTENT_URI_ROOT.buildUpon()
					.appendPath(getOrigin().abbreviation)
					.appendPath(getDestination().abbreviation).build();
		} else {
			return null;
		}
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
		if (origin != other.origin)
			return false;
		return true;
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