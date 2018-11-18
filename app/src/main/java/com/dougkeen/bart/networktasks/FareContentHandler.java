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
        if ("fare".equals(localName)) {
            Map<String, String> attributeMap = new HashMap<>();
            for (int i = attributes.getLength() - 1; i >= 0; i--) {
                attributeMap.put(attributes.getLocalName(i), attributes.getValue(i));
            }
            if (attributeMap.containsKey("class")
                    && "cash".equals(attributeMap.get("class"))
                    && attributeMap.get("amount") != null) {
                fare = "$" + attributeMap.get("amount");
            }
        }
    }
}
