package com.dougkeen.bart.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class RealTimeDepartures {
	public RealTimeDepartures(Station origin, Station destination,
			List<Route> routes) {
		this.origin = origin;
		this.destination = destination;
		this.routes = routes;
	}

	private Station origin;
	private Station destination;
	private long time;

	private List<Departure> departures;

	private List<Route> routes;

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

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public List<Departure> getDepartures() {
		if (departures == null) {
			departures = new ArrayList<Departure>();
		}
		return departures;
	}

	public void setDepartures(List<Departure> departures) {
		this.departures = departures;
	}

	public void addDeparture(Departure departure) {
		Station destination = Station.getByAbbreviation(departure
				.getDestinationAbbreviation());
		if (departure.getLine() == null)
			return;
		for (Route route : routes) {
			if (route.trainDestinationIsApplicable(destination,
					departure.getLine())) {
				departure.setRequiresTransfer(route.hasTransfer());
				getDepartures().add(departure);
				departure.calculateEstimates(time);
				return;
			}
		}
	}

	public void sortDepartures() {
		Collections.sort(getDepartures());
	}

	public List<Route> getRoutes() {
		return routes;
	}

	public void setRoutes(List<Route> routes) {
		this.routes = routes;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RealTimeDepartures [origin=");
		builder.append(origin);
		builder.append(", destination=");
		builder.append(destination);
		builder.append(", time=");
		builder.append(time);
		builder.append(", departures=");
		builder.append(departures);
		builder.append("]");
		return builder.toString();
	}
}
