package com.dougkeen.bart;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.dougkeen.bart.data.Departure;
import com.dougkeen.bart.data.RealTimeDepartures;

public class EtdContentHandler extends DefaultHandler {
	public EtdContentHandler(Station origin, Station destination,
			List<Route> routes) {
		super();
		realTimeDepartures = new RealTimeDepartures(origin, destination, routes);
	}

	private final static List<String> TAGS = Arrays.asList("date", "time",
			"abbreviation", "minutes", "platform", "direction", "length",
			"color", "hexcolor", "bikeflag");

	private RealTimeDepartures realTimeDepartures;

	public RealTimeDepartures getRealTimeDepartures() {
		return realTimeDepartures;
	}

	private String date;
	private String currentDestination;
	private String currentValue;
	private Departure currentDeparture;
	private boolean isParsingTag;

	private boolean getDestinationFromLine;

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
			if (currentDestination.equalsIgnoreCase("SPCL")) {
				getDestinationFromLine = true;
			} else {
				currentDeparture.setDestination(Station
						.getByAbbreviation(currentDestination));
			}
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
			currentDestination = currentValue;
		} else if (localName.equals("minutes")) {
			if (currentValue.equalsIgnoreCase("arrived")) {
				currentDeparture.setMinutes(0);
			} else {
				currentDeparture.setMinutes(Integer.parseInt(currentValue));
			}
		} else if (localName.equals("platform")) {
			currentDeparture.setPlatform(currentValue);
		} else if (localName.equals("direction")) {
			currentDeparture.setDirection(currentValue);
		} else if (localName.equals("length")) {
			currentDeparture.setTrainLength(Integer.parseInt(currentValue));
		} else if (localName.equals("color")) {
			try {
				if (getDestinationFromLine) {
					final Line line = Line.valueOf(currentValue);
					currentDeparture.setLine(line);
					currentDeparture.setDestination(line
							.getUsualTerminusForDirectionAndOrigin(
									currentDeparture.getDirection(),
									realTimeDepartures.getOrigin()));
				} else if (currentValue.equalsIgnoreCase("WHITE")) {
					for (Line line : Line.values()) {
						if (line.stations.indexOf(currentDeparture
								.getDestination()) >= 0
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
			currentDeparture.setDestinationColor("#ff"
					+ currentValue.substring(1));
		} else if (localName.equals("bikeflag")) {
			currentDeparture.setBikeAllowed(currentValue.equalsIgnoreCase("1"));
		} else if (localName.equals("estimate")) {
			realTimeDepartures.addDeparture(currentDeparture);
			currentDeparture = null;
			getDestinationFromLine = false;
		} else if (localName.equals("etd")) {
			currentDestination = null;
		} else if (localName.equals("station")) {
			realTimeDepartures.sortDepartures();
		}
		isParsingTag = false;
		currentValue = null;
	}
}
