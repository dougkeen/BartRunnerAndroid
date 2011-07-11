package com.dougkeen.bart;

import java.util.ArrayList;
import java.util.List;

public enum Station {
	_12TH("12th", "12th St./Oakland City Center", false, "bayf"),
	_16TH("16th", "16th St. Mission", false),
	_19TH("19th", "19th St./Oakland", false, "bayf"),
	_24TH("24th", "24th St. Mission", false),
	ASHB("ashb", "Ashby", false, "mcar"),
	BALB("balb", "Balboa Park", false),
	BAYF("bayf", "Bay Fair", true, "mcar"),
	CAST("cast", "Castro Valley", false, "bayf"),
	CIVC("civc", "Civic Center", false),
	COLS("cols", "Coliseum/Oakland Airport", true, "mcar"),
	COLM("colm", "Colma", false, "balb", "balb"),
	CONC("conc", "Concord", false, "mcar"),
	DALY("daly", "Daly City", false),
	DBRK("dbrk", "Downtown Berkeley", false, "mcar"),
	DUBL("dubl", "Dublin/Pleasanton", false, "bayf"),
	DELN("deln", "El Cerrito del Norte", false, "mcar"),
	PLZA("plza", "El Cerrito Plaza", false, "mcar"),
	EMBR("embr", "Embarcadero", false),
	FRMT("frmt", "Fremont", true, "bayf"),
	FTVL("ftvl", "Fruitvale", true, "mcar"),
	GLEN("glen", "Glen Park", false),
	HAYW("hayw", "Hayward", true, "bayf"),
	LAFY("lafy", "Lafayette", false, "mcar"),
	LAKE("lake", "Lake Merritt", true, "mcar"),
	MCAR("mcar", "MacArthur", false, "bayf"),
	MLBR("mlbr", "Millbrae", false, "balb", "balb"),
	MONT("mont", "Montgomery St.", false),
	NBRK("nbrk", "North Berkeley", false, "mcar"),
	NCON("ncon", "North Concord/Martinez", false, "mcar"),
	ORIN("orin", "Orinda", false, "mcar"),
	PITT("pitt", "Pittsburg/Bay Point", false, "mcar"),
	PHIL("phil", "Pleasant Hill", false, "mcar"),
	POWL("powl", "Powell St.", false),
	RICH("rich", "Richmond", false, "mcar"),
	ROCK("rock", "Rockridge", false, "mcar"),
	SBRN("sbrn", "San Bruno", false, "balb", "balb"),
	SANL("sanl", "San Leandro", true, "mcar"),
	SFIA("sfia", "SFO Airport", false, "sbrn", "balb"),
	SHAY("shay", "South Hayward", true, "bayf"),
	SSAN("ssan", "South San Francisco", false, "balb", "balb"),
	UCTY("ucty", "Union City", true, "bayf"),
	WCRK("wcrk", "Walnut Creek", false, "mcar"),
	WDUB("wdub", "West Dublin/Pleasanton", false, "bayf"),
	WOAK("woak", "West Oakland", false),
	SPCL("spcl", "Special", false);

	public final String abbreviation;
	public final String name;
	public final boolean invertDirection;
	protected final String inboundTransferStation;
	protected final String outboundTransferStation;

	private Station(String abbreviation, String name, boolean invertDirection) {
		this.abbreviation = abbreviation;
		this.name = name;
		this.invertDirection = invertDirection;
		this.inboundTransferStation = null;
		this.outboundTransferStation = null;
	}

	private Station(String abbreviation, String name, boolean invertDirection,
			String transferStation) {
		this.abbreviation = abbreviation;
		this.name = name;
		this.invertDirection = invertDirection;
		this.inboundTransferStation = transferStation;
		this.outboundTransferStation = null;
	}

	private Station(String abbreviation, String name, boolean invertDirection,
			String inboundTransferStation, String outboundTransferStation) {
		this.abbreviation = abbreviation;
		this.name = name;
		this.invertDirection = invertDirection;
		this.inboundTransferStation = inboundTransferStation;
		this.outboundTransferStation = outboundTransferStation;
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
