package com.soundcloud.android.image;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageUrlAdjuster {

    private final static Pattern pattern = Pattern.compile("^https?://([^\\?]+)(?:\\?.*)?");

    public String adjust(String url) {
        Matcher matcher = pattern.matcher(url);
        matcher.find();
        if(matcher.groupCount() == 1) {
            return "http://" + matcher.group(1);
        } else {
            // Should this just return the original url, in case of a non-match, instead of throwing an exception?
            throw new IllegalArgumentException("Unexpected url: " + url);
        }
    }
}
