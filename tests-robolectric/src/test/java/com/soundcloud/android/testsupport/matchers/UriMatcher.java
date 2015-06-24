package com.soundcloud.android.testsupport.matchers;

import com.pivotallabs.greatexpectations.matchers.ObjectMatcher;

import android.net.Uri;

public class UriMatcher<T extends Uri, M extends UriMatcher<T, M>> extends ObjectMatcher<T, M> {
    public boolean toEqual(String expected) {
        return actual.equals(Uri.parse(expected));
    }
}
