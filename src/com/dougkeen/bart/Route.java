package com.dougkeen.bart;

public class Route {
	private Station origin;
	private Station destination;
	private Line line;
	private boolean requiresTransfer;
	private Station transferStation;
	private String direction;

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

	public Line getLine() {
		return line;
	}

	public void setLine(Line line) {
		this.line = line;
	}

	public boolean hasTransfer() {
		return requiresTransfer;
	}

	public void setTransfer(boolean requiresTransfer) {
		this.requiresTransfer = requiresTransfer;
	}

	public Station getTransferStation() {
		return transferStation;
	}

	public void setTransferStation(Station transferStation) {
		this.transferStation = transferStation;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Route [origin=");
		builder.append(origin);
		builder.append(", destination=");
		builder.append(destination);
		builder.append(", line=");
		builder.append(line);
		builder.append(", requiresTransfer=");
		builder.append(requiresTransfer);
		builder.append(", transferStation=");
		builder.append(transferStation);
		builder.append(", direction=");
		builder.append(direction);
		builder.append("]");
		return builder.toString();
	}

	public boolean trainDestinationIsApplicable(Station lineDestination,
			Line viaLine) {
		Line routeLine = getLine();
		if (routeLine.transferLine1 != null
				&& viaLine.equals(routeLine.transferLine1)) {
			return true;
		} else if (routeLine.transferLine2 != null
				&& viaLine.equals(routeLine.transferLine2)) {
			return true;
		} else {
			int originIndex = viaLine.stations.indexOf(origin);
			int routeDestinationIndex = viaLine.stations.indexOf(destination);
			int lineDestinationIndex = viaLine.stations
					.indexOf(lineDestination);
			return routeDestinationIndex >= 0
					&& ((originIndex <= routeDestinationIndex && routeDestinationIndex <= lineDestinationIndex)
							|| (originIndex >= routeDestinationIndex && routeDestinationIndex >= lineDestinationIndex));
		}
	}

}
