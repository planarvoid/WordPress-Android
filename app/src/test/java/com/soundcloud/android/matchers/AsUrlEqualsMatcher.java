package com.soundcloud.android.matchers;

import com.google.common.collect.Multimap;
import com.soundcloud.android.utils.UriUtils;
import org.hamcrest.Description;
import org.junit.internal.matchers.TypeSafeMatcher;

import android.net.Uri;

public class AsUrlEqualsMatcher extends TypeSafeMatcher<String> {
    private String expected;

    public AsUrlEqualsMatcher(String expected) {
        this.expected = expected;
    }

    public boolean matchesSafely(String actual) {
        Uri expectedUri = Uri.parse(expected);
        Uri actualUri = Uri.parse(actual);

        final String encodedPath = actualUri.getEncodedPath();
        if (encodedPath == null){
            return expectedUri.getEncodedPath() == null;
        }

        if (!encodedPath.equals(expectedUri.getEncodedPath())) {
            return false;
        }

        Multimap<String, String> expectedParams = UriUtils.getQueryParameters(expectedUri);
        Multimap<String, String> actualParams = UriUtils.getQueryParameters(actual);
        if (expectedParams.size() != actualParams.size()) {
            return false;
        }

        for (String key : expectedParams.keySet()) {
            if (!actualUri.getQueryParameter(key).equals(expectedUri.getQueryParameter(key))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a url equal to : ");
        description.appendValue(expected);
    }
}