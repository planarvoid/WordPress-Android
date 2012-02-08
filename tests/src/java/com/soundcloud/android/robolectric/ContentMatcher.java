package com.soundcloud.android.robolectric;

import static com.soundcloud.android.Expect.expect;

import com.pivotallabs.greatexpectations.BaseMatcher;
import com.soundcloud.android.provider.Content;
import com.xtremelabs.robolectric.Robolectric;

import android.database.Cursor;

public class ContentMatcher<T extends Content, M extends ContentMatcher<T, M>> extends BaseMatcher<T,M> {

    public boolean toHaveCount(int expected) {
        Cursor c = Robolectric.application.getContentResolver().query(actual.uri, null, null, null, null);
        expect(c).not.toBeNull();
        if (c.getCount() != expected) {
            failureMessage = actual + " to have count <" + expected + "> (is " +c.getCount()+")";
            return false;
        } else {
            return true;
        }
    }

    public boolean toBeEmpty() {
        return toHaveCount(0);
    }

    public boolean toBe(T expected) {
        return actual == expected;
    }
}
