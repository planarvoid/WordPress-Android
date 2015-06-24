package com.soundcloud.android.testsupport.matchers;

import com.google.common.collect.Multimap;
import com.soundcloud.android.utils.UriUtils;
import org.hamcrest.Description;
import org.junit.internal.matchers.TypeSafeMatcher;

import android.net.Uri;

public class AsQueryStringEqualsMatcher extends TypeSafeMatcher<String> {
    public static final String BASE_URI = "http://?";
    private String expected;

    public AsQueryStringEqualsMatcher(String expected) {
        this.expected = expected;
    }

    public boolean matchesSafely(String actual) {
        Uri expectedUri = Uri.parse(BASE_URI + expected);
        Uri actualUri = Uri.parse(BASE_URI + actual);

        Multimap<String, String> expectedParams = UriUtils.getQueryParameters(expectedUri);
        Multimap<String, String> actualParams = UriUtils.getQueryParameters(actualUri);
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
        description.appendText("a query string equal to : ");
        description.appendValue(expected);
    }
}