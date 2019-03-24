package com.dougkeen.bart.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public enum Station {
    _12TH(new Builder("12th", "12th St./Oakland City Center", "12th St Oak")
            .setTransferStation("bayf")),

    _16TH(new Builder("16th", "16th St. Mission", "16th St")),

    _19TH(new Builder("19th", "19th St./Oakland", "19th St Oak")
            .setTransferStation("bayf")),

    _24TH(new Builder("24th", "24th St. Mission", "24th St")),

    ANTC(new Builder("antc", "Antioch", "Antioch")
            .setIgnoreRoutingDirection(true)
            .setTransferStation("mcar")
            .setLongStationLinger(true)
            .setDepartureEqualityTolerance(719999)),

    ASHB(new Builder("ashb", "Ashby", "Ashby")
            .setTransferStation("mcar")),

    BALB(new Builder("balb", "Balboa Park", "Balboa")),

    BAYF(new Builder("bayf", "Bay Fair", "Bay Fair")
            .setInvertDirection(true)
            .setTransferStation("mcar")),

    CAST(new Builder("cast", "Castro Valley", "Castro Vly")
            .setTransferStation("bayf")),

    CIVC(new Builder("civc", "Civic Center", "Civic Ctr")),

    COLS(new Builder("cols", "Coliseum/Oakland Airport", "Coliseum/OAK")
            .setInvertDirection(true)
            .setTransferStation("mcar")),

    COLM(new Builder("colm", "Colma", "Colma")
            .setTransferStation("balb")),

    CONC(new Builder("conc", "Concord", "Concord")
            .setTransferStation("mcar")),

    DALY(new Builder("daly", "Daly City", "Daly City")),

    DBRK(new Builder("dbrk", "Downtown Berkeley", "Dtwn Berk")
            .setTransferStation("mcar")),

    DUBL(new Builder("dubl", "Dublin/Pleasanton", "Dbln/Plsntn")
            .setIgnoreRoutingDirection(true)
            .setTransferStation("bayf")
            .setLongStationLinger(true)
            .setDepartureEqualityTolerance(719999)),

    DELN(new Builder("deln", "El Cerrito del Norte", "El Cer/Norte")
            .setTransferStation("mcar")),

    PLZA(new Builder("plza", "El Cerrito Plaza", "El Cer/Plz")
            .setTransferStation("mcar")),

    EMBR(new Builder("embr", "Embarcadero", "Embarcdro")),

    FRMT(new Builder("frmt", "Fremont", "Fremont")
            .setInvertDirection(true)
            .setTransferStation("bayf")),

    FTVL(new Builder("ftvl", "Fruitvale", "Fruitvale")
            .setInvertDirection(true)
            .setTransferStation("mcar")),

    GLEN(new Builder("glen", "Glen Park", "Glen Park")),

    HAYW(new Builder("hayw", "Hayward", "Hayward")
            .setInvertDirection(true)
            .setTransferStation("bayf")),

    LAFY(new Builder("lafy", "Lafayette", "Lafayette")
            .setTransferStation("mcar")
            .excludeFromLimitedService()),

    LAKE(new Builder("lake", "Lake Merritt", "Lk Merritt")
            .setInvertDirection(true)
            .setTransferStation("mcar")),

    MCAR(new Builder("mcar", "MacArthur", "MacArthur")
            .setTransferStation("bayf")),

    MLBR(new Builder("mlbr", "Millbrae", "Millbrae")
            .setIgnoreRoutingDirection(true)
            .setTransferStation("balb")
            .setLongStationLinger(true)
            .setDepartureEqualityTolerance(719999)),

    MONT(new Builder("mont", "Montgomery St.", "Montgomery")),

    NBRK(new Builder("nbrk", "North Berkeley", "N Berkeley")
            .setTransferStation("mcar")),

    NCON(new Builder("ncon", "North Concord/Martinez", "N Conc/Mrtnz")
            .setTransferStation("mcar")),

    ORIN(new Builder("orin", "Orinda", "Orinda")
            .setTransferStation("mcar")
            .excludeFromLimitedService()),

    PCTR(new Builder("pctr", "Pittsburg Center", "Pitt Ctr")
            .setIgnoreRoutingDirection(true)
            .setTransferStation("mcar")),

    PITT(new Builder("pitt", "Pittsburg/Bay Point", "Pitt/Bay Pt")
            .setIgnoreRoutingDirection(true)
            .setTransferStation("mcar")),

    PHIL(new Builder("phil", "Pleasant Hill", "Plsnt Hill")
            .setTransferStation("mcar")),

    POWL(new Builder("powl", "Powell St.", "Powell")),

    RICH(new Builder("rich", "Richmond", "Richmond")
            .setIgnoreRoutingDirection(true)
            .setTransferStation("mcar")
            .setLongStationLinger(true)
            .setDepartureEqualityTolerance(299999)),

    ROCK(new Builder("rock", "Rockridge", "Rockridge")
            .setTransferStation("mcar")
            .excludeFromLimitedService()),

    SBRN(new Builder("sbrn", "San Bruno", "San Bruno")
            .setTransferStation("balb")),

    SANL(new Builder("sanl", "San Leandro", "San Leandro")
            .setInvertDirection(true)
            .setTransferStation("mcar")),

    SFIA(new Builder("sfia", "SFO Airport", "SFO")
            .setTransferStation("balb")
            .setLongStationLinger(true)
            .setDepartureEqualityTolerance(719999)),

    SHAY(new Builder("shay", "South Hayward", "S Hayward")
            .setInvertDirection(true)
            .setTransferStation("bayf")),

    SSAN(new Builder("ssan", "South San Francisco", "S San Fran")
            .setTransferStation("balb")),

    UCTY(new Builder("ucty", "Union City", "Union City")
            .setInvertDirection(true)
            .setTransferStation("bayf")),

    WARM(new Builder("warm", "Warm Springs/South Fremont", "Warm Springs")
            .setInvertDirection(true)
            .setIgnoreRoutingDirection(true)
            .setTransferStation("bayf")
            .setLongStationLinger(true)
            .setDepartureEqualityTolerance(299999)),

    WCRK(new Builder("wcrk", "Walnut Creek", "Walnut Crk")
            .setTransferStation("mcar")
            .excludeFromLimitedService()),

    WDUB(new Builder("wdub", "West Dublin/Pleasanton", "W Dbln/Plsntn")
            .setTransferStation("bayf")),

    WOAK(new Builder("woak", "West Oakland", "W Oakland")),

    SPCL(new Builder("spcl", "Special", "Special"));

    public final String abbreviation;
    public final String name;
    public final String shortName;
    public final boolean transferFriendly;
    public final boolean invertDirection;
    protected final String inboundTransferStation;
    protected final String outboundTransferStation;
    public final boolean ignoreRoutingDirection;
    public final boolean longStationLinger;
    public final int departureEqualityTolerance;
    public final boolean includedInLimitedService;

    public final static int DEFAULT_DEPARTURE_EQUALITY_TOLERANCE = 119999;

    private static class Builder {

        private final String abbreviation;
        private final String name;
        private final String shortName;
        private boolean invertDirection = false;
        private String inboundTransferStation = null;
        private String outboundTransferStation = null;
        private boolean ignoreRoutingDirection = false;
        private boolean longStationLinger = false;
        private int departureEqualityTolerance = DEFAULT_DEPARTURE_EQUALITY_TOLERANCE;
        private boolean includedInLimitedService = true;

        public Builder(String abbreviation, String name, String shortName) {
            this.abbreviation = abbreviation;
            this.name = name;
            this.shortName = shortName;
        }

        public Builder setInvertDirection(boolean invertDirection) {
            this.invertDirection = invertDirection;
            return this;
        }

        public Builder setIgnoreRoutingDirection(boolean ignoreRoutingDirection) {
            this.ignoreRoutingDirection = ignoreRoutingDirection;
            return this;
        }

        public Builder setTransferStation(String transferStation) {
            this.inboundTransferStation = transferStation;
            this.outboundTransferStation = transferStation;
            return this;
        }

        public Builder setInboundTransferStation(String inboundTransferStation) {
            this.inboundTransferStation = inboundTransferStation;
            return this;
        }

        public Builder setOutboundTransferStation(String outboundTransferStation) {
            this.outboundTransferStation = outboundTransferStation;
            return this;
        }

        public Builder setLongStationLinger(boolean longStationLinger) {
            this.longStationLinger = longStationLinger;
            return this;
        }

        public Builder setDepartureEqualityTolerance(int departureEqualityTolerance) {
            this.departureEqualityTolerance = departureEqualityTolerance;
            return this;
        }

        public Builder excludeFromLimitedService() {
            this.includedInLimitedService = false;
            return this;
        }
    }

    Station(Builder builder) {
        this.abbreviation = builder.abbreviation;
        this.name = builder.name;
        this.shortName = builder.shortName;
        this.invertDirection = builder.invertDirection;
        this.inboundTransferStation = builder.inboundTransferStation;
        this.outboundTransferStation = builder.outboundTransferStation;
        this.ignoreRoutingDirection = builder.ignoreRoutingDirection;
        this.longStationLinger = builder.longStationLinger;
        this.departureEqualityTolerance = builder.departureEqualityTolerance;
        this.includedInLimitedService = builder.includedInLimitedService;
        this.transferFriendly = this.outboundTransferStation != null;
    }

    public static Station getByAbbreviation(String abbr) {
        try {
            if (abbr == null) {
                return null;
            } else if (Character.isDigit(abbr.charAt(0))) {
                return Station.valueOf("_" + abbr.toUpperCase());
            } else {
                return Station.valueOf(abbr.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            Log.e(Constants.TAG, "Could not find station for '" + abbr + "'", e);
            return null;
        }
    }

    public static Station getByApproximateName(String name) {
        if (name == null) return null;

        final String lowercaseName = name.toLowerCase();
        for (Station station : Station.values()) {
            if (lowercaseName.startsWith(station.name.toLowerCase())) {
                return station;
            }
        }
        for (Station station : Station.values()) {
            if (lowercaseName.endsWith(station.name.toLowerCase())) {
                return station;
            }
        }
        return Station.SPCL;
    }

    public Station getInboundTransferStation() {
        return getByAbbreviation(inboundTransferStation);
    }

    public Station getOutboundTransferStation() {
        return getByAbbreviation(outboundTransferStation);
    }

    public boolean isValidEndpointForDestination(Station dest, Station endpoint) {
        for (Line line : Line.values()) {
            int origIndex = line.stations.indexOf(this);
            if (origIndex < 0)
                continue;
            int destIndex = line.stations.indexOf(dest);
            if (destIndex < 0)
                continue;
            int endpointIndex = line.stations.indexOf(endpoint);
            if (endpointIndex >= 0)
                return true;
        }
        return false;
    }

    public List<Route> getDirectRoutesForDestination(Station dest) {
        return getDirectRoutesForDestination(this, dest, null, null);
    }

    public List<Route> getDirectRoutesForDestination(Station origin,
                                                     Station dest, Station transferStation,
                                                     Collection<Line> transferLines) {
        if (dest == null)
            return null;
        Boolean isNorth = null;
        List<Route> returnList = new ArrayList<Route>();
        final Collection<Line> applicableLines = Line.getLinesWithStations(
                this, dest);
        if (transferLines != null && !transferLines.isEmpty()) {
            for (Line transferLine : transferLines) {
                int origIndex = transferLine.stations.indexOf(origin);
                int destIndex = transferLine.stations.indexOf(origin
                        .getOutboundTransferStation());

                isNorth = (origIndex < destIndex);
                if (origin.invertDirection && transferLine.directionMayInvert) {
                    isNorth = !isNorth;
                    break;
                }
            }
        }
        for (Line line : applicableLines) {
            if (transferLines == null || transferLines.isEmpty()) {
                int origIndex = line.stations.indexOf(this);
                int destIndex = line.stations.indexOf(dest);

                isNorth = (origIndex < destIndex);
                if (line.directionMayInvert && this.invertDirection) {
                    isNorth = !isNorth;
                }
            }
            Route route = new Route();
            route.setOrigin(origin);
            route.setDirectLine(line);
            if (this.equals(origin)) {
                route.setDestination(dest);
            } else {
                // This must be the outbound transfer station
                route.setDestination(origin.getOutboundTransferStation());
                route.setTransferLines(transferLines);
            }
            route.setDirection(isNorth ? "n" : "s");
            if (transferStation != null || line.requiresTransfer) {
                route.setTransfer(true);
            } else {
                route.setTransfer(false);
            }

            returnList.add(route);
        }
        return returnList;
    }

    public List<Route> getTransferRoutes(Station dest) {
        List<Route> returnList = new ArrayList<Route>();

        if (dest.getInboundTransferStation() != null) {
            // Try getting to the destination's inbound xfer station first
            returnList.addAll(getDirectRoutesForDestination(this,
                    dest.getInboundTransferStation(),
                    dest.getInboundTransferStation(), null));
        }

        if (returnList.isEmpty() && outboundTransferStation != null) {
            // Try getting from the outbound transfer station to the
            // destination next
            final Collection<Line> outboundTransferLines = Line
                    .getLinesWithStations(this, getOutboundTransferStation());
            final List<Route> routesForDestination = getOutboundTransferStation()
                    .getDirectRoutesForDestination(this, dest,
                            getOutboundTransferStation(), outboundTransferLines);
            if (routesForDestination != null && !routesForDestination.isEmpty()) {
                returnList.addAll(routesForDestination);
            }
        }

        if (returnList.isEmpty()) {
            // Try getting from the outbound transfer station to the
            // destination's inbound xfer station
            final List<Route> routesForDestination = getDoubleTransferRoutes(dest);
            if (routesForDestination != null && !routesForDestination.isEmpty()) {
                returnList.addAll(routesForDestination);
            }
        }

        return returnList;
    }

    public List<Route> getDoubleTransferRoutes(Station dest) {
        if (getOutboundTransferStation() == null
                || dest.getInboundTransferStation() == null)
            return new ArrayList<Route>();

        // Get routes from the outbound transfer station to the
        // destination's inbound xfer station
        return getOutboundTransferStation().getDirectRoutesForDestination(this,
                dest.getInboundTransferStation(), getOutboundTransferStation(),
                Line.getLinesWithStations(this, getOutboundTransferStation()));
    }

    static public List<Station> getStationList() {
        List<Station> list = new ArrayList<Station>();
        for (Station station : values()) {
            if (!station.equals(Station.SPCL)) {
                list.add(station);
            }
        }
        return list;
    }

    public String toString() {
        return name;
    }

    public boolean isBetween(Station origin, Station destination, Line line) {
        int originIndex = line.stations.indexOf(origin);
        int destinationIndex = line.stations.indexOf(destination);
        int stationIndex = line.stations.indexOf(this);
        if (originIndex < 0 || destinationIndex < 0 || stationIndex < 0) {
            return false;
        }

        return Math.abs(stationIndex - originIndex) < Math.abs(destinationIndex
                - originIndex);
    }
}
