package com.dougkeen.bart;

import java.util.ArrayList;
import java.util.List;

public enum Station {
	_12TH("12th", "12th St./Oakland City Center", false, false, "bayf"),
	_16TH("16th", "16th St. Mission", false, false),
	_19TH("19th", "19th St./Oakland", false, false, "bayf"),
	_24TH("24th", "24th St. Mission", false, false),
	ASHB("ashb", "Ashby", false, false, "mcar"),
	BALB("balb", "Balboa Park", false, false),
	BAYF("bayf", "Bay Fair", true, false, "mcar"),
	CAST("cast", "Castro Valley", false, false, "bayf"),
	CIVC("civc", "Civic Center", false, false),
	COLS("cols", "Coliseum/Oakland Airport", true, false, "mcar"),
	COLM("colm", "Colma", false, false, "balb", "balb"),
	CONC("conc", "Concord", false, false, "mcar"),
	DALY("daly", "Daly City", false, false),
	DBRK("dbrk", "Downtown Berkeley", false, false, "mcar"),
	DUBL("dubl", "Dublin/Pleasanton", false, true, "bayf"),
	DELN("deln", "El Cerrito del Norte", false, false, "mcar"),
	PLZA("plza", "El Cerrito Plaza", false, false, "mcar"),
	EMBR("embr", "Embarcadero", false, false),
	FRMT("frmt", "Fremont", true, false, "bayf"),
	FTVL("ftvl", "Fruitvale", true, false, "mcar"),
	GLEN("glen", "Glen Park", false, false),
	HAYW("hayw", "Hayward", true, false, "bayf"),
	LAFY("lafy", "Lafayette", false, false, "mcar"),
	LAKE("lake", "Lake Merritt", true, false, "mcar"),
	MCAR("mcar", "MacArthur", false, false, "bayf"),
	MLBR("mlbr", "Millbrae", false, true, "balb", "balb"),
	MONT("mont", "Montgomery St.", false, false),
	NBRK("nbrk", "North Berkeley", false, false, "mcar"),
	NCON("ncon", "North Concord/Martinez", false, false, "mcar"),
	ORIN("orin", "Orinda", false, false, "mcar"),
	PITT("pitt", "Pittsburg/Bay Point", false, true, "mcar"),
	PHIL("phil", "Pleasant Hill", false, false, "mcar"),
	POWL("powl", "Powell St.", false, false),
	RICH("rich", "Richmond", false, true, "mcar"),
	ROCK("rock", "Rockridge", false, false, "mcar"),
	SBRN("sbrn", "San Bruno", false, false, "balb", "balb"),
	SANL("sanl", "San Leandro", true, false, "mcar"),
	SFIA("sfia", "SFO Airport", false, false, "sbrn", "balb"),
	SHAY("shay", "South Hayward", true, false, "bayf"),
	SSAN("ssan", "South San Francisco", false, false, "balb", "balb"),
	UCTY("ucty", "Union City", true, false, "bayf"),
	WCRK("wcrk", "Walnut Creek", false, false, "mcar"),
	WDUB("wdub", "West Dublin/Pleasanton", false, false, "bayf"),
	WOAK("woak", "West Oakland", false, false),
	SPCL("spcl", "Special", false, false);

	public final String abbreviation;
	public final String name;
	public final boolean invertDirection;
	protected final String inboundTransferStation;
	protected final String outboundTransferStation;
	public final boolean endOfLine;

	private Station(String abbreviation, String name, boolean invertDirection, boolean endOfLine) {
		this.abbreviation = abbreviation;
		this.name = name;
		this.invertDirection = invertDirection;
		this.inboundTransferStation = null;
		this.outboundTransferStation = null;
		this.endOfLine = endOfLine;
	}

	private Station(String abbreviation, String name, boolean invertDirection, boolean endOfLine,
			String transferStation) {
		this.abbreviation = abbreviation;
		this.name = name;
		this.invertDirection = invertDirection;
		this.inboundTransferStation = transferStation;
		this.outboundTransferStation = null;
		this.endOfLine = endOfLine;
	}

	private Station(String abbreviation, String name, boolean invertDirection, boolean endOfLine,
			String inboundTransferStation, String outboundTransferStation) {
		this.abbreviation = abbreviation;
		this.name = name;
		this.invertDirection = invertDirection;
		this.inboundTransferStation = inboundTransferStation;
		this.outboundTransferStation = outboundTransferStation;
		this.endOfLine = endOfLine;
	}

	public static Station getByAbbreviation(String abbr) {
		if (abbr == null) {
			return null;
		} else if (Character.isDigit(abbr.charAt(0))) {
			return Station.valueOf("_" + abbr.toUpperCase());
		} else {
			return Station.valueOf(abbr.toUpperCase());
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
			returnList.add(route);
		}
		if (isNorth == null) {
			if (outboundTransferStation != null) {
				returnList
						.addAll(getOutboundTransferStation()
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
