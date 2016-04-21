package com.dougkeen.bart.networktasks;

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
    private static final List<String> TAGS = Arrays.asList("date", "time",
            "abbreviation", "minutes", "platform", "direction", "length",
            "color", "hexcolor", "bikeflag", "destination");

    private RealTimeDepartures realTimeDepartures;

    private String date;
    private String currentDestinationAbbreviation;
    private String currentDestinationName;
    private String currentValue;
    private Departure currentDeparture;
    private boolean isParsingTag;

    public EtdContentHandler(Station origin, Station destination,
                             List<Route> routes) {
        super();
        realTimeDepartures = new RealTimeDepartures(origin, destination, routes);
    }

    public RealTimeDepartures getRealTimeDepartures() {
        return realTimeDepartures;
    }

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
        if (localName.equals("estimate")) {
            currentDeparture = new Departure();
            Station destination = Station.getByAbbreviation(currentDestinationAbbreviation);
            if (destination == null) {
                destination = Station.getByApproximateName(currentDestinationName);
            }
            currentDeparture.setTrainDestination(destination);
            currentDeparture.setOrigin(realTimeDepartures.getOrigin());
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (localName.equals("date")) {
            date = currentValue;
        } else if (localName.equals("time")) {
            realTimeDepartures.setTime(Date.parse(date + " " + currentValue));
        } else if (localName.equals("abbreviation")) {
            currentDestinationAbbreviation = currentValue;
        } else if (localName.equals("destination")) {
            currentDestinationName = currentValue;
        } else if (localName.equals("minutes")) {
            if (StringUtils.isNumeric(currentValue)) {
                currentDeparture.setMinutes(Integer.parseInt(currentValue));
            } else {
                currentDeparture.setMinutes(0);
            }
        } else if (localName.equals("platform")) {
            currentDeparture.setPlatform(currentValue);
        } else if (localName.equals("direction")) {
            currentDeparture.setDirection(currentValue);
        } else if (localName.equals("length")) {
            currentDeparture.setTrainLength(currentValue);
        } else if (localName.equals("color")) {
            try {
                if (currentValue.equalsIgnoreCase("WHITE")) {
                    for (Line line : Line.values()) {
                        if (line.stations.indexOf(currentDeparture
                                .getTrainDestination()) >= 0
                                && line.stations.indexOf(realTimeDepartures
                                .getDestination()) >= 0) {
                            currentDeparture.setLine(line);
                            break;
                        }
                    }
                } else {
                    currentDeparture.setLine(Line.valueOf(currentValue));
                }
            } catch (IllegalArgumentException e) {
                Log.w(Constants.TAG, "There is no line called '" + currentValue
                        + "'");
            }
        } else if (localName.equals("hexcolor")) {
            currentDeparture.setTrainDestinationColor("#ff"
                    + currentValue.substring(1));
        } else if (localName.equals("bikeflag")) {
            currentDeparture.setBikeAllowed(currentValue.equalsIgnoreCase("1"));
        } else if (localName.equals("estimate")) {
            // If we can't infer a real destination because of weird API data,
            // just skip it ¯\_(ツ)_/¯
            if (realTimeDepartures.getDestination() != null) {
                realTimeDepartures.addDeparture(currentDeparture);
            }
            currentDeparture = null;
        } else if (localName.equals("etd")) {
            currentDestinationAbbreviation = null;
        } else if (localName.equals("station")) {
            realTimeDepartures.finalizeDeparturesList();
        }
        isParsingTag = false;
        currentValue = null;
    }
}
