package com.soundcloud.android.service.beta;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class BucketParser {
    public final static String DATE_FORMAT_STR_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public final static DateFormat DATE_FORMAT_ISO8601;

    static {
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        DATE_FORMAT_ISO8601 = new SimpleDateFormat(DATE_FORMAT_STR_ISO8601);
        DATE_FORMAT_ISO8601.setTimeZone(gmt);
    }

    public static List<Beta> getContent(InputStream body) throws IOException {
        try {
            final List<Beta> content;
            XmlPullParser parser = getParser();
            parser.setInput(body, "UTF-8");
            content = new ArrayList<Beta>();
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("Contents".equals(parser.getName())) {
                        content.add(parseContent(parser));
                    }
                }
                eventType = parser.next();
            }
            return content;
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }
    }

    private static Beta parseContent(XmlPullParser parser) throws IOException, XmlPullParserException {
        final Beta c = new Beta();
        int eventType;
        while ((eventType = parser.next()) != XmlPullParser.END_TAG && !"Contents".equals(parser.getName())) {
            if (eventType == XmlPullParser.START_TAG) {
                if ("Key".equals(parser.getName())) {
                    c.key = parser.nextText();
                } else if ("LastModified".equals(parser.getName())) {
                    c.lastmodified = parseISO8601(parser.nextText());
                } else if ("ETag".equals(parser.getName())) {
                    String tag = parser.nextText();
                    c.etag = tag.substring(1, tag.length() - 1);
                } else if ("Size".equals(parser.getName())) {
                    c.size = Long.parseLong(parser.nextText());
                } else if ("StorageClass".equals(parser.getName())) {
                    c.storageClass = parser.nextText();
                }
            }
        }
        return c;
    }

    private static long parseISO8601(String date) {
        try {
            return DATE_FORMAT_ISO8601.parse(date).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    public static List<Beta> getContent(String body) throws IOException {
        return getContent(new ByteArrayInputStream(body.getBytes()));
    }

    private static XmlPullParser getParser() throws XmlPullParserException {
        return XmlPullParserFactory.newInstance().newPullParser();
    }
}
