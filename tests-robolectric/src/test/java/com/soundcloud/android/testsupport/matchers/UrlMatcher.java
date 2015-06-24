package com.soundcloud.android.testsupport.matchers;

import com.google.common.collect.Multimap;
import com.soundcloud.android.utils.UriUtils;
import org.hamcrest.Description;
import org.junit.internal.matchers.TypeSafeMatcher;

import android.net.Uri;

public class UrlMatcher extends TypeSafeMatcher<String> {
    private String expected;
    private String failContext;

    public UrlMatcher(String expected) {
        this.expected = expected;
    }

    public boolean matchesSafely(String actual) {
        Uri expectedUri = Uri.parse(expected);
        Uri actualUri = Uri.parse(actual);

        final String scheme = actualUri.getScheme();
        if (!scheme.equals(expectedUri.getScheme())) {
            this.failContext = scheme;
            return false;
        }

        final String host = actualUri.getHost();
        if (!host.equals(expectedUri.getHost())) {
            this.failContext = host;
            return false;
        }

        final String encodedPath = actualUri.getEncodedPath();
        if (encodedPath == null) {
            this.failContext = "path was null";
            return expectedUri.getEncodedPath() == null;
        } else if (!encodedPath.equals(expectedUri.getEncodedPath())) {
            this.failContext = encodedPath;
            return false;
        }

        Multimap<String, String> expectedParams = UriUtils.getQueryParameters(expectedUri);
        Multimap<String, String> actualParams = UriUtils.getQueryParameters(actual);
        if (expectedParams.size() != actualParams.size()) {
            this.failContext = "different number of query params";
            return false;
        }

        for (String key : expectedParams.keySet()) {
            final String actualParam = actualUri.getQueryParameter(key);
            final String expectedParam = expectedUri.getQueryParameter(key);
            if (actualParam == null) {
                this.failContext = key + " is missing" + "; expected " + key + "=" + expectedParam;
                return false;
            }
            if (!actualParam.equals(expectedParam)) {
                this.failContext = key + "=" + actualParam + "; expected " + key + "=" + expectedParam;
                return false;
            }
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(expected);
        if (failContext != null) {
            description.appendText("\nMismatch in: '").appendText(failContext).appendText("'");
        }
    }
}