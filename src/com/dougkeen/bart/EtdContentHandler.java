package com.dougkeen.bart;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.dougkeen.bart.data.Arrival;
import com.dougkeen.bart.data.RealTimeArrivals;

public class EtdContentHandler extends DefaultHandler {
	public EtdContentHandler(Station origin, Station destination,
			List<Route> routes) {
		super();
		realTimeArrivals = new RealTimeArrivals(origin, destination, routes);
	}

	private final static List<String> TAGS = Arrays.asList("date", "time",
			"abbreviation", "minutes", "platform", "direction", "length",
			"hexcolor", "bikeflag");

	private RealTimeArrivals realTimeArrivals;

	public RealTimeArrivals getRealTimeArrivals() {
		return realTimeArrivals;
	}

	private String date;
	private String currentDestination;
	private String currentValue;
	private Arrival currentArrival;
	private boolean isParsingTag;

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
			currentArrival = new Arrival();
			currentArrival.setDestination(Station
					.getByAbbreviation(currentDestination));
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (localName.equals("date")) {
			date = currentValue;
		} else if (localName.equals("time")) {
			realTimeArrivals.setTime(Date.parse(date + " " + currentValue));
		} else if (localName.equals("abbreviation")) {
			currentDestination = currentValue;
		} else if (localName.equals("minutes")) {
			if (currentValue.equalsIgnoreCase("arrived")) {
				currentArrival.setMinutes(0);
			} else {
				currentArrival.setMinutes(Integer.parseInt(currentValue));
			}
		} else if (localName.equals("platform")) {
			currentArrival.setPlatform(currentValue);
		} else if (localName.equals("direction")) {
			currentArrival.setDirection(currentValue);
		} else if (localName.equals("length")) {
			currentArrival.setTrainLength(Integer.parseInt(currentValue));
		} else if (localName.equals("hexcolor")) {
			currentArrival.setDestinationColor("#ff"
					+ currentValue.substring(1));
		} else if (localName.equals("bikeflag")) {
			currentArrival.setBikeAllowed(currentValue.equalsIgnoreCase("1"));
		} else if (localName.equals("estimate")) {
			realTimeArrivals.addArrival(currentArrival);
			currentArrival = null;
		} else if (localName.equals("etd")) {
			currentDestination = null;
		} else if (localName.equals("station")) {
			realTimeArrivals.sortArrivals();
		}
		isParsingTag = false;
		currentValue = null;
	}
}
