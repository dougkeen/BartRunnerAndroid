package com.dougkeen.bart.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private boolean transfersIncluded = false;

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

    public boolean areTransfersIncluded() {
        return transfersIncluded;
    }

    public Departure getEarliestDirectDeparture() {
        Departure earliest = null;
        for (Departure departure : getDepartures()) {
            if (!departure.getRequiresTransfer() && (earliest == null || departure.getMinutes() < earliest.getMinutes())) {
                earliest = departure;
            }
        }
        return earliest;
    }

    public Departure getEarliestTransferDeparture() {
        List<Route> xferRoutes = origin.getTransferRoutes(destination);
        List<Departure> xferDepartures = new ArrayList<>();
        for (Departure departure : unfilteredDepartures) {
            Route route = findRouteForDeparture(departure, xferRoutes);
            if (route != null && route.hasTransfer()) {
                xferDepartures.add(departure);
            }
        }
        Departure earliest = null;
        for (Departure departure : xferDepartures) {
            if (earliest == null || departure.getMinutes() < earliest.getMinutes()) {
                earliest = departure;
            }
        }
        return earliest;
    }

    public void includeTransferRoutes() {
        transfersIncluded = true;
        routes.addAll(origin.getTransferRoutes(destination));
        rebuildFilteredDeparturesCollection();
    }

    public void includeDoubleTransferRoutes() {
        transfersIncluded = true;
        routes.addAll(origin.getDoubleTransferRoutes(destination));
        rebuildFilteredDeparturesCollection();
    }

    /**
     * This is a temporary fix until BART API fixes their implementation of `dir` filters in API urls
     *
     * @param direction "n" or "s"
     */
    public void filterByDirection(String direction) {
        if (direction == null || direction.isEmpty()) return;
        Iterator<Departure> iterator = unfilteredDepartures.iterator();
        while (iterator.hasNext()) {
            Departure departure = iterator.next();
            if (!departure.getDirection().toLowerCase().startsWith(direction)) {
                Log.v(Constants.TAG, "Removing departure in wrong direction: " + departure);
                iterator.remove();
            }
        }
        rebuildFilteredDeparturesCollection();
    }

    private void rebuildFilteredDeparturesCollection() {
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
        Route route = findRouteForDeparture(departure, this.routes);
        if (route != null) {
            departure.setRequiresTransfer(route.hasTransfer());
            departure.setTransferScheduled(Line.YELLOW_ORANGE_SCHEDULED_TRANSFER.equals(route.getDirectLine()));
            getDepartures().add(departure);
            departure.calculateEstimates(time);
        }
    }

    private Route findRouteForDeparture(Departure departure, List<Route> routes) {
        Station destination = Station.getByAbbreviation(departure
                .getTrainDestinationAbbreviation());
        if (departure.getLine() == null)
            return null;
        for (Route route : routes) {
            if (route.trainDestinationIsApplicable(destination, departure.getLine())
                    && (route.getDestination().includedInLimitedService || !departure.isLimited())) {
                return route;
            }
        }
        return null;
    }

    public void sortDepartures() {
        Collections.sort(departures, new Comparator<Departure>() {
            @Override
            public int compare(Departure o1, Departure o2) {
                return o1.getMinutes() - o2.getMinutes();
            }
        });
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
