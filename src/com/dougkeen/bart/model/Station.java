package com.dougkeen.bart.model;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public enum Station {
	_12TH("12th", "12th St./Oakland City Center", true, false, false, "bayf"),
	_16TH("16th", "16th St. Mission", false, false, false),
	_19TH("19th", "19th St./Oakland", true, false, false, "bayf"),
	_24TH("24th", "24th St. Mission", false, false, false),
	ASHB("ashb", "Ashby", true, false, false, "mcar"),
	BALB("balb", "Balboa Park", false, false, false),
	BAYF("bayf", "Bay Fair", true, true, false, "mcar"),
	CAST("cast", "Castro Valley", true, false, false, "bayf"),
	CIVC("civc", "Civic Center", false, false, false),
	COLS("cols", "Coliseum/Oakland Airport", true, true, false, "mcar"),
	COLM("colm", "Colma", true, false, false, "balb", "balb"),
	CONC("conc", "Concord", true, false, false, "mcar"),
	DALY("daly", "Daly City", false, false, false),
	DBRK("dbrk", "Downtown Berkeley", true, false, false, "mcar"),
	DUBL("dubl", "Dublin/Pleasanton", true, false, true, "bayf", "bayf", true,
			719999),
	DELN("deln", "El Cerrito del Norte", true, false, false, "mcar"),
	PLZA("plza", "El Cerrito Plaza", true, false, false, "mcar"),
	EMBR("embr", "Embarcadero", false, false, false),
	FRMT("frmt", "Fremont", true, true, true, "bayf", "bayf", true, 299999),
	FTVL("ftvl", "Fruitvale", true, true, false, "mcar"),
	GLEN("glen", "Glen Park", false, false, false),
	HAYW("hayw", "Hayward", true, true, false, "bayf"),
	LAFY("lafy", "Lafayette", true, false, false, "mcar"),
	LAKE("lake", "Lake Merritt", true, true, false, "mcar"),
	MCAR("mcar", "MacArthur", true, false, false, "bayf"),
	MLBR("mlbr", "Millbrae", true, false, true, "balb", "balb", true, 719999),
	MONT("mont", "Montgomery St.", false, false, false),
	NBRK("nbrk", "North Berkeley", true, false, false, "mcar"),
	NCON("ncon", "North Concord/Martinez", true, false, false, "mcar"),
	ORIN("orin", "Orinda", true, false, false, "mcar"),
	PITT("pitt", "Pittsburg/Bay Point", true, false, true, "mcar", "mcar",
			true, 719999),
	PHIL("phil", "Pleasant Hill", true, false, false, "mcar"),
	POWL("powl", "Powell St.", false, false, false),
	RICH("rich", "Richmond", true, false, true, "mcar", "mcar", true, 299999),
	ROCK("rock", "Rockridge", true, false, false, "mcar"),
	SBRN("sbrn", "San Bruno", true, false, false, "balb", "balb"),
	SANL("sanl", "San Leandro", true, true, false, "mcar"),
	SFIA("sfia", "SFO Airport", true, false, false, "sbrn", "balb", true,
			719999),
	SHAY("shay", "South Hayward", true, true, false, "bayf"),
	SSAN("ssan", "South San Francisco", true, false, false, "balb", "balb"),
	UCTY("ucty", "Union City", true, true, false, "bayf"),
	WCRK("wcrk", "Walnut Creek", true, false, false, "mcar"),
	WDUB("wdub", "West Dublin/Pleasanton", true, false, false, "bayf"),
	WOAK("woak", "West Oakland", false, false, false),
	SPCL("spcl", "Special", false, false, false);

	public final String abbreviation;
	public final String name;
	public final boolean transferFriendly;
	public final boolean invertDirection;
	protected final String inboundTransferStation;
	protected final String outboundTransferStation;
	public final boolean endOfLine;
	public final boolean longStationLinger;
	public final int departureEqualityTolerance;

	public final static int DEFAULT_DEPARTURE_EQUALITY_TOLERANCE = 59999;

	private Station(String abbreviation, String name, boolean transferFriendly,
			boolean invertDirection, boolean endOfLine) {
		this(abbreviation, name, transferFriendly, invertDirection, endOfLine,
				null, null, false, DEFAULT_DEPARTURE_EQUALITY_TOLERANCE);
	}

	private Station(String abbreviation, String name, boolean transferFriendly,
			boolean invertDirection, boolean endOfLine, String transferStation) {
		this(abbreviation, name, transferFriendly, invertDirection, endOfLine,
				transferStation, null, false,
				DEFAULT_DEPARTURE_EQUALITY_TOLERANCE);
	}

	private Station(String abbreviation, String name, boolean transferFriendly,
			boolean invertDirection, boolean endOfLine,
			String inboundTransferStation, String outboundTransferStation) {
		this(abbreviation, name, transferFriendly, invertDirection, endOfLine,
				inboundTransferStation, outboundTransferStation, false,
				DEFAULT_DEPARTURE_EQUALITY_TOLERANCE);
	}

	private Station(String abbreviation, String name, boolean transferFriendly,
			boolean invertDirection, boolean endOfLine,
			String inboundTransferStation, String outboundTransferStation,
			boolean longStationLinger, int departureEqualityTolerance) {
		this.abbreviation = abbreviation;
		this.name = name;
		this.invertDirection = invertDirection;
		this.inboundTransferStation = inboundTransferStation;
		this.transferFriendly = transferFriendly;
		this.outboundTransferStation = outboundTransferStation;
		this.endOfLine = endOfLine;
		this.longStationLinger = longStationLinger;
		this.departureEqualityTolerance = departureEqualityTolerance;
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

	public List<Route> getRoutesForDestination(Station dest) {
		return getRoutesForDestination(dest, null);
	}

	public List<Route> getRoutesForDestination(Station dest,
			Station transferStation) {
		if (dest == null)
			return null;
		Boolean isNorth = null;
		List<Route> returnList = new ArrayList<Route>();
		for (Line line : Line.values()) {
			int origIndex = line.stations.indexOf(this);
			if (origIndex < 0)
				continue;
			int destIndex = line.stations.indexOf(dest);
			if (destIndex < 0)
				continue;

			isNorth = (origIndex < destIndex);
			if (line.directionMayInvert && this.invertDirection) {
				isNorth = !isNorth;
			}
			Route route = new Route();
			route.setOrigin(this);
			route.setDestination(dest);
			route.setDirection(isNorth ? "n" : "s");
			route.setLine(line);
			if (transferStation != null || line.requiresTransfer) {
				route.setTransfer(true);
			} else {
				route.setTransfer(false);
			}

			if (route.hasTransfer() && !transferFriendly
					&& !Line.YELLOW_ORANGE_SCHEDULED_TRANSFER.equals(line)) {
				continue;
			}

			returnList.add(route);
		}
		if (isNorth == null) {
			if (outboundTransferStation != null) {
				returnList.addAll(getOutboundTransferStation()
						.getRoutesForDestination(dest,
								getOutboundTransferStation()));
			} else if (dest.getInboundTransferStation() != null) {
				final List<Route> routesForDestination = getRoutesForDestination(
						dest.getInboundTransferStation(),
						dest.getInboundTransferStation());
				if (routesForDestination != null
						&& !routesForDestination.isEmpty()) {
					returnList.addAll(routesForDestination);
				}
			}
		}
		return returnList;
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
}
