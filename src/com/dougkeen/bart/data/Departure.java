package com.dougkeen.bart.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.dougkeen.bart.Line;
import com.dougkeen.bart.Station;

public class Departure implements Parcelable, Comparable<Departure> {
	private static final int MINIMUM_MERGE_OVERLAP_MILLIS = 10000;

	public Departure() {
		super();
	}

	public Departure(String destinationAbbr, String destinationColor,
			String platform, String direction, boolean bikeAllowed,
			String trainLength, int minutes) {
		super();
		this.destination = Station.getByAbbreviation(destinationAbbr);
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

	private Station destination;
	private Line line;
	private String destinationColor;
	private String platform;
	private String direction;
	private boolean bikeAllowed;
	private String trainLength;
	private boolean requiresTransfer;

	private int minutes;

	private long minEstimate;
	private long maxEstimate;

	public Station getDestination() {
		return destination;
	}

	public void setDestination(Station destination) {
		this.destination = destination;
	}

	public String getDestinationName() {
		if (destination != null)
			return destination.name;
		return null;
	}

	public String getDestinationAbbreviation() {
		if (destination != null)
			return destination.abbreviation;
		return null;
	}

	public Line getLine() {
		return line;
	}

	public void setLine(Line line) {
		this.line = line;
	}

	public String getDestinationColor() {
		return destinationColor;
	}

	public void setDestinationColor(String destinationColor) {
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
		return trainLength + " car train";
	}

	public boolean getRequiresTransfer() {
		return requiresTransfer;
	}

	public void setRequiresTransfer(boolean requiresTransfer) {
		this.requiresTransfer = requiresTransfer;
	}

	public int getMinutes() {
		return minutes;
	}

	public void setMinutes(int minutes) {
		this.minutes = minutes;
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
		return (int) (((getMinEstimate() + getMaxEstimate()) / 2 - System
				.currentTimeMillis()) / 1000);
	}

	public boolean hasDeparted() {
		return getMeanSecondsLeft() < 0;
	}

	public void calculateEstimates(long originalEstimateTime) {
		setMinEstimate(originalEstimateTime + (getMinutes() * 60 * 1000)
				- (30000));
		setMaxEstimate(getMinEstimate() + 60000);
	}

	public void mergeEstimate(Departure departure) {
		if ((getMaxEstimate() - departure.getMinEstimate()) < MINIMUM_MERGE_OVERLAP_MILLIS
				|| departure.getMaxEstimate() - getMinEstimate() < MINIMUM_MERGE_OVERLAP_MILLIS) {
			// The estimate must have changed... just use the latest incoming
			// values
			setMinEstimate(departure.getMinEstimate());
			setMaxEstimate(departure.getMaxEstimate());
			return;
		}

		final long newMin = Math.max(getMinEstimate(),
				departure.getMinEstimate());
		final long newMax = Math.min(getMaxEstimate(),
				departure.getMaxEstimate());
		if (newMax > newMin) { // We can never have 0 or negative uncertainty
			setMinEstimate(newMin);
			setMaxEstimate(newMax);
		}
	}

	public int compareTo(Departure another) {
		return (this.getMinutes() > another.getMinutes()) ? 1 : ((this
				.getMinutes() == another.getMinutes()) ? 0 : -1);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (bikeAllowed ? 1231 : 1237);
		result = prime * result
				+ ((destination == null) ? 0 : destination.hashCode());
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
		if (destination != other.destination)
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
		if (maxEstimate != other.maxEstimate)
			return false;
		if (minEstimate != other.minEstimate)
			return false;
		if (minutes != other.minutes)
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

	public String getCountdownText() {
		StringBuilder builder = new StringBuilder();
		int secondsLeft = getMeanSecondsLeft();
		if (hasDeparted()) {
			builder.append("Departed");
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(destination);
		if (requiresTransfer) {
			builder.append(" (w/ xfer)");
		}
		builder.append(", ");
		builder.append(getCountdownText());
		return builder.toString();
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(destination.abbreviation);
		dest.writeString(destinationColor);
		dest.writeString(platform);
		dest.writeString(direction);
		dest.writeByte((byte) (bikeAllowed ? 1 : 0));
		dest.writeString(trainLength);
		dest.writeByte((byte) (requiresTransfer ? 1 : 0));
		dest.writeInt(minutes);
		dest.writeLong(minEstimate);
		dest.writeLong(maxEstimate);
	}

	private void readFromParcel(Parcel in) {
		destination = Station.getByAbbreviation(in.readString());
		destinationColor = in.readString();
		platform = in.readString();
		direction = in.readString();
		bikeAllowed = in.readByte() != 0;
		trainLength = in.readString();
		requiresTransfer = in.readByte() != 0;
		minutes = in.readInt();
		minEstimate = in.readLong();
		maxEstimate = in.readLong();
	}

	public static final Parcelable.Creator<Departure> CREATOR = new Parcelable.Creator<Departure>() {
		public Departure createFromParcel(Parcel in) {
			return new Departure(in);
		}

		public Departure[] newArray(int size) {
			return new Departure[size];
		}
	};
}