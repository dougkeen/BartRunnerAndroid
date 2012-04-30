package com.dougkeen.bart.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.util.Log;

public enum Station {
	_12TH("12th", "12th St./Oakland City Center", false, false, "bayf", "bayf"),
	_16TH("16th", "16th St. Mission", false, false),
	_19TH("19th", "19th St./Oakland", false, false, "bayf", "bayf"),
	_24TH("24th", "24th St. Mission", false, false),
	ASHB("ashb", "Ashby", false, false, "mcar", "mcar"),
	BALB("balb", "Balboa Park", false, false),
	BAYF("bayf", "Bay Fair", true, false, "mcar", "mcar"),
	CAST("cast", "Castro Valley", false, false, "bayf", "bayf"),
	CIVC("civc", "Civic Center", false, false),
	COLS("cols", "Coliseum/Oakland Airport", true, false, "mcar", "mcar"),
	COLM("colm", "Colma", false, false, "balb", "balb"),
	CONC("conc", "Concord", false, false, "mcar", "mcar"),
	DALY("daly", "Daly City", false, false),
	DBRK("dbrk", "Downtown Berkeley", false, false, "mcar", "mcar"),
	DUBL("dubl", "Dublin/Pleasanton", false, true, "bayf", "bayf", true, 719999),
	DELN("deln", "El Cerrito del Norte", false, false, "mcar", "mcar"),
	PLZA("plza", "El Cerrito Plaza", false, false, "mcar", "mcar"),
	EMBR("embr", "Embarcadero", false, false),
	FRMT("frmt", "Fremont", true, true, "bayf", "bayf", true, 299999),
	FTVL("ftvl", "Fruitvale", true, false, "mcar", "mcar"),
	GLEN("glen", "Glen Park", false, false),
	HAYW("hayw", "Hayward", true, false, "bayf", "bayf"),
	LAFY("lafy", "Lafayette", false, false, "mcar", "mcar"),
	LAKE("lake", "Lake Merritt", true, false, "mcar", "mcar"),
	MCAR("mcar", "MacArthur", false, false, "bayf", "bayf"),
	MLBR("mlbr", "Millbrae", false, true, "balb", "balb", true, 719999),
	MONT("mont", "Montgomery St.", false, false),
	NBRK("nbrk", "North Berkeley", false, false, "mcar", "mcar"),
	NCON("ncon", "North Concord/Martinez", false, false, "mcar", "mcar"),
	ORIN("orin", "Orinda", false, false, "mcar", "mcar"),
	PITT("pitt", "Pittsburg/Bay Point", false, true, "mcar", "mcar", true,
			719999),
	PHIL("phil", "Pleasant Hill", false, false, "mcar", "mcar"),
	POWL("powl", "Powell St.", false, false),
	RICH("rich", "Richmond", false, true, "mcar", "mcar", true, 299999),
	ROCK("rock", "Rockridge", false, false, "mcar", "mcar"),
	SBRN("sbrn", "San Bruno", false, false, "balb", "balb"),
	SANL("sanl", "San Leandro", true, false, "mcar", "mcar"),
	SFIA("sfia", "SFO Airport", false, false, "balb", "balb", true, 719999),
	SHAY("shay", "South Hayward", true, false, "bayf", "bayf"),
	SSAN("ssan", "South San Francisco", false, false, "balb", "balb"),
	UCTY("ucty", "Union City", true, false, "bayf", "bayf"),
	WCRK("wcrk", "Walnut Creek", false, false, "mcar", "mcar"),
	WDUB("wdub", "West Dublin/Pleasanton", false, false, "bayf", "bayf"),
	WOAK("woak", "West Oakland", false, false),
	SPCL("spcl", "Special", false, false);

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

	private Station(String abbreviation, String name, boolean invertDirection,
			boolean endOfLine) {
		this(abbreviation, name, invertDirection, endOfLine, null, null, false,
				DEFAULT_DEPARTURE_EQUALITY_TOLERANCE);
	}

	private Station(String abbreviation, String name, boolean invertDirection,
			boolean endOfLine, String transferStation) {
		this(abbreviation, name, invertDirection, endOfLine, transferStation,
				null, false, DEFAULT_DEPARTURE_EQUALITY_TOLERANCE);
	}

	private Station(String abbreviation, String name, boolean invertDirection,
			boolean endOfLine, String inboundTransferStation,
			String outboundTransferStation) {
		this(abbreviation, name, invertDirection, endOfLine,
				inboundTransferStation, outboundTransferStation, false,
				DEFAULT_DEPARTURE_EQUALITY_TOLERANCE);
	}

	private Station(String abbreviation, String name, boolean invertDirection,
			boolean endOfLine, String inboundTransferStation,
			String outboundTransferStation, boolean longStationLinger,
			int departureEqualityTolerance) {
		this.abbreviation = abbreviation;
		this.name = name;
		this.invertDirection = invertDirection;
		this.inboundTransferStation = inboundTransferStation;
		this.transferFriendly = outboundTransferStation != null;
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
}
