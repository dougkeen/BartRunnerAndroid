package com.dougkeen.bart.networktasks;

import android.util.Log;

import com.dougkeen.bart.model.Constants;
import com.dougkeen.bart.model.ScheduleInformation;
import com.dougkeen.bart.model.ScheduleItem;
import com.dougkeen.bart.model.Station;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class ScheduleContentHandler extends DefaultHandler {
    public ScheduleContentHandler(Station origin, Station destination) {
        super();
        schedule = new ScheduleInformation(origin, destination);
    }

    private final static List<String> TAGS = Arrays.asList("date", "time",
            "trip", "leg");

    private final static DateFormat TRIP_DATE_FORMAT;
    private final static DateFormat REQUEST_DATE_FORMAT;

    private final static TimeZone PACIFIC_TIME = TimeZone
            .getTimeZone("America/Los_Angeles");

    static {
        TRIP_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy h:mm a");
        REQUEST_DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy h:mm a");

        TRIP_DATE_FORMAT.setTimeZone(PACIFIC_TIME);
        REQUEST_DATE_FORMAT.setTimeZone(PACIFIC_TIME);
    }

    private ScheduleInformation schedule;

    public ScheduleInformation getSchedule() {
        return schedule;
    }

    private String currentValue;
    private boolean isParsingTag;

    private String requestDate;
    private String requestTime;

    private ScheduleItem currentTrip;

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
        final int numberOfAttributes = attributes.getLength();
        if ("trip".equals(localName)) {
            currentTrip = new ScheduleItem();
            String originDate = null;
            String originTime = null;
            String destinationDate = null;
            String destinationTime = null;
            for (int i = 0; i < numberOfAttributes; i++) {
                if ("origin".equalsIgnoreCase(attributes.getLocalName(i))) {
                    currentTrip.setOrigin(Station.getByAbbreviation(attributes
                            .getValue(i)));
                } else if ("destination".equalsIgnoreCase(attributes.getLocalName(i))) {
                    currentTrip.setDestination(Station
                            .getByAbbreviation(attributes.getValue(i)));
                } else if ("fare".equalsIgnoreCase(attributes.getLocalName(i))) {
                    currentTrip.setFare(attributes.getValue(i));
                } else if ("origTimeMin".equalsIgnoreCase(attributes.getLocalName(i))) {
                    originTime = attributes.getValue(i);
                } else if ("origTimeDate".equalsIgnoreCase(attributes.getLocalName(i))) {
                    originDate = attributes.getValue(i);
                } else if ("destTimeMin".equalsIgnoreCase(attributes.getLocalName(i))) {
                    destinationTime = attributes.getValue(i);
                } else if ("destTimeDate".equalsIgnoreCase(attributes.getLocalName(i))) {
                    destinationDate = attributes.getValue(i);
                } else if ("bikeFlag".equalsIgnoreCase(attributes.getLocalName(i))) {
                    currentTrip.setBikesAllowed("1".equals(attributes.getValue(i)));
                }
            }

            long departTime = parseDate(TRIP_DATE_FORMAT, originDate,
                    originTime);
            if (departTime > 0)
                currentTrip.setDepartureTime(departTime);

            long arriveTime = parseDate(TRIP_DATE_FORMAT, destinationDate,
                    destinationTime);
            if (arriveTime > 0)
                currentTrip.setArrivalTime(arriveTime);

            schedule.addTrip(currentTrip);
        }
        if ("leg".equals(localName)) {
            String legNumber = null;
            for (int i = 0; i < numberOfAttributes; i++) {
                if ("order".equals(attributes.getLocalName(i))) {
                    legNumber = attributes.getValue(i);
                } else if ("trainHeadStation".equals(attributes.getLocalName(i)) && "1".equals(legNumber)) {
                    currentTrip.setTrainHeadStation(attributes.getValue(i)
                            .toLowerCase());
                }
            }
        }
    }

    private long parseDate(DateFormat format, String dateString,
                           String timeString) {
        if (dateString == null || timeString == null) {
            return -1;
        }
        try {
            return format.parse(dateString + " " + timeString).getTime();
        } catch (ParseException e) {
            Log.e(Constants.TAG, "Unable to parse datetime '" + dateString
                    + " " + timeString + "'", e);
            return -1;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if ("date".equals(localName)) {
            requestDate = currentValue;
        } else if ("time".equals(localName)) {
            requestTime = currentValue;
        }
        isParsingTag = false;
        currentValue = null;
    }

    @Override
    public void endDocument() {
        long date = parseDate(REQUEST_DATE_FORMAT, requestDate, requestTime);
        if (date > 0) {
            schedule.setDate(date);
        }
    }
}
