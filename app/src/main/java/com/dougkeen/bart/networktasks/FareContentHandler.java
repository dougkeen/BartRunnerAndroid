package com.dougkeen.bart.networktasks;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;

public class FareContentHandler extends DefaultHandler {
    public FareContentHandler() {
        super();
    }

    private String fare;

    public String getFare() {
        return fare;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        if (localName.equals("fare")) {
            Map<String, String> attributeMap = new HashMap<>();
            for (int i = attributes.getLength() - 1; i >= 0; i--) {
                attributeMap.put(attributes.getLocalName(i), attributes.getValue(i));
            }
            if (attributeMap.containsKey("class")
                    && attributeMap.get("class").equals("cash")
                    && attributeMap.get("amount") != null) {
                fare = "$" + attributeMap.get("amount");
            }
        }
    }
}
