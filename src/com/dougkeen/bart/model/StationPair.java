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
}