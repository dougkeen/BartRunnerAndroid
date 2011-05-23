package com.dougkeen.bart.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.dougkeen.bart.Route;
import com.dougkeen.bart.Station;

public class RealTimeArrivals {
	public RealTimeArrivals(Station origin, Station destination,
			List<Route> routes) {
		this.origin = origin;
		this.destination = destination;
		this.routes = routes;
	}

	private Station origin;
	private Station destination;
	private long time;

	private List<Arrival> arrivals;

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

	public List<Arrival> getArrivals() {
		if (arrivals == null) {
			arrivals = new ArrayList<Arrival>();
		}
		return arrivals;
	}

	public void setArrivals(List<Arrival> arrivals) {
		this.arrivals = arrivals;
	}

	public void addArrival(Arrival arrival) {
		Station destination = Station.getByAbbreviation(arrival
				.getDestinationAbbreviation());
		for (Route route : routes) {
			if (route.getLine().trainDestinationIsApplicable(destination)) {
				arrival.setRequiresTransfer(route.hasTransfer());
				getArrivals().add(arrival);
				arrival.calculateEstimates(time);
				return;
			}
		}
	}

	public void sortArrivals() {
		Collections.sort(getArrivals());
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
		builder.append("RealTimeArrivals [origin=");
		builder.append(origin);
		builder.append(", destination=");
		builder.append(destination);
		builder.append(", time=");
		builder.append(time);
		builder.append(", arrivals=");
		builder.append(arrivals);
		builder.append("]");
		return builder.toString();
	}
}
