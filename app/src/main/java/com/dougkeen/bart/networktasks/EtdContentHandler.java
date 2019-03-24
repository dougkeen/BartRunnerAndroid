package com.dougkeen.bart.networktasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.Departure;
import com.dougkeen.bart.model.Line;
import com.dougkeen.bart.model.RealTimeDepartures;
import com.dougkeen.bart.model.Route;
import com.dougkeen.bart.model.Station;

public class EtdContentHandler extends DefaultHandler {
    public EtdContentHandler(Station origin, Station destination,
                             List<Route> routes) {
        super();
        realTimeDepartures = new RealTimeDepartures(origin, destination, routes);
    }

    private final static List<String> TAGS = Arrays.asList("date", "time",
            "abbreviation", "minutes", "platform", "direction", "length",
            "color", "hexcolor", "bikeflag", "destination", "error", "limited");

    private RealTimeDepartures realTimeDepartures;

    public RealTimeDepartures getRealTimeDepartures() {
        return realTimeDepartures;
    }

    private String date;
    private String currentDestinationAbbreviation;
    private String currentDestinationName;
    private boolean currentDestinationLimited = false;
    private String currentValue;
    private Departure currentDeparture;
    private boolean isParsingTag;
    private String error;

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (isParsingTag) {
            currentValue = new String(ch, start, length);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        if (TAGS.contains(localName)) {
            isParsingTag = true;
        }
        if ("estimate".equals(localName)) {
            currentDeparture = new Departure();
            Station destination = Station.getByAbbreviation(currentDestinationAbbreviation);
            if (destination == null) {
                destination = Station.getByApproximateName(currentDestinationName);
            }
            currentDeparture.setTrainDestination(destination);
            currentDeparture.setOrigin(realTimeDepartures.getOrigin());
            currentDeparture.setLimited(currentDestinationLimited);
        }
    }

    private boolean oscillator = false;

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if ("date".equals(localName)) {
            date = currentValue;
        } else if ("time".equals(localName)) {
            realTimeDepartures.setTime(Date.parse(date + " " + currentValue));
        } else if ("abbreviation".equals(localName)) {
            currentDestinationAbbreviation = currentValue;
        } else if ("destination".equals(localName)) {
            currentDestinationName = currentValue;
        } else if ("limited".equals(localName)) {
            currentDestinationLimited = "1".equals(currentValue);
        } else if ("minutes".equals(localName)) {
            if (StringUtils.isNumeric(currentValue)) {
                currentDeparture.setMinutes(Integer.parseInt(currentValue));
            } else {
                currentDeparture.setMinutes(0);
            }
        } else if ("platform".equals(localName)) {
            currentDeparture.setPlatform(currentValue);
        } else if ("direction".equals(localName)) {
            currentDeparture.setDirection(currentValue);
        } else if ("length".equals(localName)) {
            currentDeparture.setTrainLength(currentValue);
        } else if ("color".equals(localName)) {
            currentDeparture.setTrainDestinationColorText(currentValue);
        } else if ("hexcolor".equals(localName)) {
            currentDeparture.setTrainDestinationColorHex("#ff"
                    + currentValue.substring(1));
        } else if ("bikeflag".equals(localName)) {
            currentDeparture.setBikeAllowed(currentValue.equalsIgnoreCase("1"));
        } else if ("estimate".equals(localName)) {
            // If we can't infer a real destination because of weird API data,
            // just skip it ¯\_(ツ)_/¯
            if (realTimeDepartures.getDestination() != null) {
                // Infer line
                String lineColor = currentDeparture.getTrainDestinationColorText();
                try {
                    Line selectedLine = null;
                    if ("WHITE".equalsIgnoreCase(lineColor)) {
                        selectedLine = guessLine();
                    } else {
                        try {
                            selectedLine = Line.valueOf(lineColor);
                        } catch (IllegalArgumentException e) {
                            selectedLine = guessLine();
                        }
                    }
                    if (selectedLine == null || !selectedLine.containsStation(currentDeparture.getTrainDestination())) {
                        selectedLine = guessLine();
                    }
                    currentDeparture.setLine(selectedLine);
                } catch (IllegalArgumentException e) {
                    Log.w(Constants.TAG, "There is no line called '" + currentValue
                            + "'");
                }

                realTimeDepartures.addDeparture(currentDeparture);
            }
            currentDeparture = null;
        } else if ("etd".equals(localName)) {
            currentDestinationAbbreviation = null;
        } else if ("station".equals(localName)) {
            realTimeDepartures.finalizeDeparturesList();
        } else if ("error".equals(localName)) {
            error = currentValue;
        }
        isParsingTag = false;
        currentValue = null;
    }

    private Line guessLine() {
        for (Line line : Line.values()) {
            if (line.stations.indexOf(currentDeparture.getTrainDestination()) >= 0
                    && line.stations.indexOf(realTimeDepartures.getOrigin()) >= 0
            ) {
                return line;
            }
        }
        return null;
    }

    public String getError() {
        return error;
    }
}
