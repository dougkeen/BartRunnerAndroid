package com.dougkeen.bart.networktasks;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;
import android.util.Xml;

import com.dougkeen.bart.model.Alert;

public class AlertListConverter extends
        AbstractHttpMessageConverter<Alert.AlertList> {

    @Override
    protected Alert.AlertList readInternal(
            Class<? extends Alert.AlertList> clazz,
            HttpInputMessage inputMessage) throws IOException,
            HttpMessageNotReadableException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputMessage.getBody(), writer, "UTF-8");

        String xml = writer.toString();
        if (xml.length() == 0) {
            throw new IOException("Server returned blank xml document");
        }

        AlertListHandler handler = new AlertListHandler();
        try {
            Xml.parse(xml, handler);
        } catch (SAXException e) {
            Log.e("AlertListConverter", "XML parsing error", e);
            return null;
        }

        return handler.getAlertList();
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Alert.AlertList.class.equals(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        final List<MediaType> supportedMediaTypes = new ArrayList<MediaType>();
        supportedMediaTypes.add(MediaType.TEXT_HTML);
        supportedMediaTypes.add(MediaType.TEXT_XML);
        supportedMediaTypes.addAll(super.getSupportedMediaTypes());
        return supportedMediaTypes;
    }

    @Override
    protected void writeInternal(Alert.AlertList arg0, HttpOutputMessage arg1)
            throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedOperationException();
    }

    private static class AlertListHandler extends DefaultHandler {
        private final static List<String> TAGS = Arrays.asList("bsa", "type",
                "description", "posted", "expires");

        private String currentValue;
        private boolean isParsingTag;
        private Alert currentAlert;
        private Alert.AlertList returnList = new Alert.AlertList();

        public Alert.AlertList getAlertList() {
            return returnList;
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
            final int numberOfAttributes = attributes.getLength();
            if (localName.equals("bsa")) {
                for (int i = 0; i < numberOfAttributes; i++) {
                    if (attributes.getLocalName(i).equalsIgnoreCase("id")) {
                        currentAlert = new Alert(attributes.getValue(i));
                        break;
                    }
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            if (currentAlert != null) {
                if (localName.equals("type")) {
                    currentAlert.setType(currentValue);
                } else if (localName.equals("description")) {
                    currentAlert.setDescription(currentValue);
                } else if (localName.equals("posted")) {
                    currentAlert.setPostedTime(currentValue);
                } else if (localName.equals("expires")) {
                    currentAlert.setExpiresTime(currentValue);
                } else if (localName.equals("bsa")) {
                    returnList.addAlert(currentAlert);
                    currentAlert = null;
                }
            }
            isParsingTag = false;
            currentValue = null;
        }

        @Override
        public void endDocument() throws SAXException {
            super.endDocument();
            if (!returnList.hasAlerts()) {
                returnList.setNoDelaysReported(true);
            }
        }

    }
}