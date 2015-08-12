package com.dougkeen.bart.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public enum Line {
    RED(false, Station.MLBR, Station.SBRN, Station.SSAN, Station.COLM,
            Station.DALY, Station.BALB, Station.GLEN, Station._24TH,
            Station._16TH, Station.CIVC, Station.POWL, Station.MONT,
            Station.EMBR, Station.WOAK, Station._12TH, Station._19TH,
            Station.MCAR, Station.ASHB, Station.DBRK, Station.NBRK,
            Station.PLZA, Station.DELN, Station.RICH),
    ORANGE(false, Station.FRMT, Station.UCTY, Station.SHAY, Station.HAYW,
            Station.BAYF, Station.SANL, Station.COLS, Station.FTVL,
            Station.LAKE, Station._12TH, Station._19TH, Station.MCAR,
            Station.ASHB, Station.DBRK, Station.NBRK, Station.PLZA,
            Station.DELN, Station.RICH),
    YELLOW(false, Station.MLBR, Station.SFIA, Station.SBRN, Station.SSAN,
            Station.COLM, Station.DALY, Station.BALB, Station.GLEN,
            Station._24TH, Station._16TH, Station.CIVC, Station.POWL,
            Station.MONT, Station.EMBR, Station.WOAK, Station._12TH,
            Station._19TH, Station.MCAR, Station.ROCK, Station.ORIN,
            Station.LAFY, Station.WCRK, Station.PHIL, Station.CONC,
            Station.NCON, Station.PITT),
    BLUE(true, Station.DALY, Station.BALB, Station.GLEN, Station._24TH,
            Station._16TH, Station.CIVC, Station.POWL, Station.MONT,
            Station.EMBR, Station.WOAK, Station.LAKE, Station.FTVL,
            Station.COLS, Station.SANL, Station.BAYF, Station.CAST,
            Station.WDUB, Station.DUBL),
    GREEN(true, Station.DALY, Station.BALB, Station.GLEN, Station._24TH,
            Station._16TH, Station.CIVC, Station.POWL, Station.MONT,
            Station.EMBR, Station.WOAK, Station.LAKE, Station.FTVL,
            Station.COLS, Station.SANL, Station.BAYF, Station.HAYW,
            Station.SHAY, Station.UCTY, Station.FRMT),
    YELLOW_ORANGE_SCHEDULED_TRANSFER(YELLOW, ORANGE, Station.MLBR,
            Station.SFIA, Station.SBRN, Station.SSAN, Station.COLM,
            Station.DALY, Station.BALB, Station.GLEN, Station._24TH,
            Station._16TH, Station.CIVC, Station.POWL, Station.MONT,
            Station.EMBR, Station.WOAK, Station.ASHB, Station.DBRK,
            Station.NBRK, Station.PLZA, Station.DELN, Station.RICH);

    public final List<Station> stations;

    protected final boolean directionMayInvert;

    protected final boolean requiresTransfer;

    protected final Line transferLine1;

    protected final Line transferLine2;

    Line(boolean directionMayInvert, Station... stationArray) {
        this.requiresTransfer = false;
        this.directionMayInvert = directionMayInvert;
        stations = Arrays.asList(stationArray);
        this.transferLine1 = null;
        this.transferLine2 = null;
    }

    Line(Line transferLine1, Line transferLine2,
         Station... stationArray) {
        this.requiresTransfer = true;
        this.directionMayInvert = false;
        stations = Arrays.asList(stationArray);
        this.transferLine1 = transferLine1;
        this.transferLine2 = transferLine2;
    }

    Line(boolean directionMayInvert, Line transferLine1,
         Line transferLine2, Station... stationArray) {
        this.requiresTransfer = true;
        this.directionMayInvert = directionMayInvert;
        stations = Arrays.asList(stationArray);
        this.transferLine1 = transferLine1;
        this.transferLine2 = transferLine2;
    }

    public static Collection<Line> getLinesForStation(Station station) {
        Collection<Line> lines = new ArrayList<Line>();
        for (Line line : Line.values()) {
            if (line.stations.contains(station)) {
                lines.add(line);
            }
        }
        return lines;
    }

    public static Collection<Line> getLinesWithStations(Station station1,
                                                        Station station2) {
        Collection<Line> lines = new ArrayList<Line>();
        for (Line line : Line.values()) {
            if (line.stations.contains(station1)
                    && line.stations.contains(station2)) {
                lines.add(line);
            }
        }
        return lines;
    }

    public static Set<Station> getPotentialDestinations(Station station) {
        Set<Station> destinations = new TreeSet<Station>();

        for (Line line : getLinesForStation(station)) {
            destinations.addAll(line.stations);
        }

        destinations.remove(station);

        return destinations;
    }
}
