package com.dougkeen.bart.model;

public class StationPair {
	public StationPair(Station origin, Station destination) {
		super();
		this.origin = origin;
		this.destination = destination;
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

}