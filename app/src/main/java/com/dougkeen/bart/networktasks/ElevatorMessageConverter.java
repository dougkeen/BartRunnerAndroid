package com.dougkeen.bart.networktasks;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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

public class ElevatorMessageConverter extends
        AbstractHttpMessageConverter<String> {

    private static final String TAG = "ElevatorMessageConverter";

    @Override
    protected String readInternal(Class<? extends String> clazz,
                                  HttpInputMessage inputMessage) throws IOException,
            HttpMessageNotReadableException {

        final ElevatorMessageHandler handler = new ElevatorMessageHandler();
        try {
            Xml.parse(new InputStreamReader(inputMessage.getBody()), handler);
        } catch (SAXException e) {
            Log.e(TAG, "Unable to parse elevator message", e);
            return null;
        }

        return handler.getMessage();
    }

    @Override
    protected boolean supports(Class<?> arg0) {
        return String.class.equals(arg0);
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
    protected void writeInternal(String arg0, HttpOutputMessage arg1)
            throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedOperationException();
    }

    private static class ElevatorMessageHandler extends DefaultHandler {
        private String currentValue;
        private boolean isParsingTag;

        private String message;

        public String getMessage() {
            return message;
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
            if ("description".equals(localName)) {
                isParsingTag = true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            if ("description".equals(localName)) {
                message = currentValue;
            }
            isParsingTag = false;
            currentValue = null;
        }
    }

}
