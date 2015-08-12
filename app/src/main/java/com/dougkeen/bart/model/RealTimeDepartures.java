package com.dougkeen.bart.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class RealTimeDepartures {
    public RealTimeDepartures(Station origin, Station destination,
                              List<Route> routes) {
        this.origin = origin;
        this.destination = destination;
        this.routes = routes;
        this.unfilteredDepartures = new ArrayList<Departure>();
    }

    private Station origin;
    private Station destination;
    private long time;

    private List<Departure> departures;

    final private List<Departure> unfilteredDepartures;

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

    public void includeTransferRoutes() {
        routes.addAll(origin.getTransferRoutes(destination));
        rebuildFilteredDepaturesCollection();
    }

    public void includeDoubleTransferRoutes() {
        routes.addAll(origin.getDoubleTransferRoutes(destination));
        rebuildFilteredDepaturesCollection();
    }

    private void rebuildFilteredDepaturesCollection() {
        getDepartures().clear();
        for (Departure departure : unfilteredDepartures) {
            addDepartureIfApplicable(departure);
        }
    }

    public void addDeparture(Departure departure) {
        unfilteredDepartures.add(departure);
        addDepartureIfApplicable(departure);
    }

    private void addDepartureIfApplicable(Departure departure) {
        Station destination = Station.getByAbbreviation(departure
                .getTrainDestinationAbbreviation());
        if (departure.getLine() == null)
            return;
        for (Route route : routes) {
            if (route.trainDestinationIsApplicable(destination,
                    departure.getLine())) {
                departure.setRequiresTransfer(route.hasTransfer());
                departure
                        .setTransferScheduled(Line.YELLOW_ORANGE_SCHEDULED_TRANSFER
                                .equals(route.getDirectLine()));
                getDepartures().add(departure);
                departure.calculateEstimates(time);
                return;
            }
        }
    }

    public void sortDepartures() {
        Collections.sort(getDepartures());
    }

    public void finalizeDeparturesList() {
        boolean hasDirectRoute = false;
        for (Departure departure : getDepartures()) {
            if (!departure.getRequiresTransfer()) {
                hasDirectRoute = true;
                break;
            }
        }
        if (hasDirectRoute) {
            Iterator<Departure> iterator = getDepartures().iterator();
            while (iterator.hasNext()) {
                Departure departure = iterator.next();
                if (departure.getRequiresTransfer()
                        && (!departure.isTransferScheduled() || departure
                        .getTrainDestination().isBetween(getOrigin(),
                                getDestination(), departure.getLine()))) {
                    iterator.remove();
                }
            }
        }
        sortDepartures();
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
