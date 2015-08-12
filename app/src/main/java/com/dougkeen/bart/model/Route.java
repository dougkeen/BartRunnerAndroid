package com.dougkeen.bart.model;

import java.util.Collection;

public class Route {
    private Station origin;
    private Station destination;
    private Line directLine;
    private Collection<Line> transferLines;
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

    public Line getDirectLine() {
        return directLine;
    }

    public void setDirectLine(Line line) {
        this.directLine = line;
    }

    public Collection<Line> getTransferLines() {
        return transferLines;
    }

    public void setTransferLines(Collection<Line> transferLines) {
        this.transferLines = transferLines;
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
        builder.append(directLine);
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
        Line routeLine = getDirectLine();
        if (routeLine.transferLine1 != null
                && viaLine.equals(routeLine.transferLine1)) {
            return true;
        } else if (routeLine.transferLine2 != null
                && viaLine.equals(routeLine.transferLine2)) {
            return true;
        } else if (requiresTransfer && transferLines != null
                && !transferLines.isEmpty()) {
            return transferLines.contains(viaLine);
        } else {
            int originIndex = viaLine.stations.indexOf(origin);
            int routeDestinationIndex = viaLine.stations.indexOf(destination);
            int lineDestinationIndex = viaLine.stations
                    .indexOf(lineDestination);
            return routeDestinationIndex >= 0
                    && ((originIndex <= routeDestinationIndex && routeDestinationIndex <= lineDestinationIndex) || (originIndex >= routeDestinationIndex && routeDestinationIndex >= lineDestinationIndex));
        }
    }

}
