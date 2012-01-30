package com.soundcloud.android.robolectric;

import static com.soundcloud.android.Expect.expect;

import com.pivotallabs.greatexpectations.matchers.ObjectMatcher;
import com.xtremelabs.robolectric.Robolectric;

import android.database.Cursor;
import android.net.Uri;

public class UriMatcher<T extends Uri, M extends UriMatcher<T, M>> extends ObjectMatcher<T, M> {
    public boolean toEqual(String expected) {
        return actual.equals(Uri.parse(expected));
    }

    public boolean toHaveCount(int expected) {
        Cursor c = Robolectric.application.getContentResolver().query(actual, null, null, null, null);
        expect(c).not.toBeNull();
        return c.getCount() == expected;
    }

    public boolean toBeEmpty() {
        return toHaveCount(0);
    }
}
