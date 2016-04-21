package com.dougkeen.bart.networktasks;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FareContentHandler extends DefaultHandler {
    private String currentValue;
    private boolean isParsingTag;
    private String fare;

    public FareContentHandler() {
        super();
    }

    public String getFare() {
        return fare;
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
        if (localName.equals("fare")) {
            isParsingTag = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (localName.equals("fare")) {
            fare = "$" + currentValue;
        }
        isParsingTag = false;
        currentValue = null;
    }

}
