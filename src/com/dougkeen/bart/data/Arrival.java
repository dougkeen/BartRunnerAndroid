package com.dougkeen.bart.data;

import com.dougkeen.bart.Station;

public class Arrival implements Comparable<Arrival> {
	public Arrival() {
		super();
	}

	public Arrival(String destinationAbbr, String destinationColor,
			String platform, String direction, boolean bikeAllowed,
			int trainLength, int minutes) {
		super();
		this.destination = Station.getByAbbreviation(destinationAbbr);
		this.destinationColor = destinationColor;
		this.platform = platform;
		this.direction = direction;
		this.bikeAllowed = bikeAllowed;
		this.trainLength = trainLength;
		this.minutes = minutes;
	}

	private Station destination;
	private String destinationColor;
	private String platform;
	private String direction;
	private boolean bikeAllowed;
	private int trainLength;
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

	public int getTrainLength() {
		return trainLength;
	}

	public void setTrainLength(int trainLength) {
		this.trainLength = trainLength;
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

	public void calculateEstimates(long originalEstimateTime) {
		setMinEstimate(originalEstimateTime + (getMinutes() * 60 * 1000));
		setMaxEstimate(getMinEstimate() + (59 * 1000));
	}

	public void mergeEstimate(Arrival arrival) {
		setMinEstimate(Math.max(getMinEstimate(), arrival.getMinEstimate()));
		setMaxEstimate(Math.min(getMaxEstimate(), arrival.getMaxEstimate()));
	}

	@Override
	public int compareTo(Arrival another) {
		return (this.getMinutes() > another.getMinutes()) ? 1 : (
				(this.getMinutes() == another.getMinutes()) ? 0 : -1);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Arrival other = (Arrival) obj;
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
		if (platform == null) {
			if (other.platform != null)
				return false;
		} else if (!platform.equals(other.platform))
			return false;
		if (trainLength != other.trainLength)
			return false;

		long delta = (getMinEstimate() - other.getMinEstimate());
		return delta > -60000 && delta < 60000;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(destination);
		if(requiresTransfer) {
			builder.append(" (w/ xfer)");
		}
		builder.append(", ");
		builder.append(trainLength);
		int secondsLeft = getMeanSecondsLeft();
		if (getMinutes() == 0 || secondsLeft < 0) {
			builder.append(" car train has arrived");
		} else {
			builder.append(" car train in ");
			builder.append(secondsLeft / 60);
			builder.append("m, ");
			builder.append(secondsLeft % 60);
			builder.append("s, ±");
			builder.append(getUncertaintySeconds());
			builder.append("s");
		}
		return builder.toString();
	}
}
