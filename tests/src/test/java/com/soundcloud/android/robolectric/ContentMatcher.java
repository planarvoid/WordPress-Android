package com.soundcloud.android.robolectric;

import static com.soundcloud.android.Expect.expect;

import com.pivotallabs.greatexpectations.BaseMatcher;
import com.soundcloud.android.provider.Content;
import com.xtremelabs.robolectric.Robolectric;

import android.database.Cursor;

public class ContentMatcher<T extends Content, M extends ContentMatcher<T, M>> extends BaseMatcher<T,M> {

    public boolean toHaveCount(int expected) {
        Cursor cursor = Robolectric.application.getContentResolver().query(actual.uri, null, null, null, null);
        expect(cursor).not.toBeNull();
        if (cursor.getCount() != expected) {
            failureMessage = actual + " to have count <" + expected + "> (is " +cursor.getCount()+")";
            cursor.close();
            return false;
        } else {
            cursor.close();
            return true;
        }
    }

    public boolean toBeEmpty() {
        return toHaveCount(0);
    }

    public boolean toBe(T expected) {
        return actual == expected;
    }

    public boolean toHaveColumnAt(int position, String columnName, int columnValue) {
        Cursor cursor = Robolectric.application.getContentResolver().query(actual.uri, null, null, null, null);
        expect(cursor).not.toBeNull();
        expect(cursor.moveToPosition(position)).toBeTrue();
        expect(cursor).toHaveColumn(columnName, columnValue);
        cursor.close();
        return true;
    }

    public boolean toHaveColumnAt(int position, String columnName, long columnValue) {
        Cursor cursor = Robolectric.application.getContentResolver().query(actual.uri, null, null, null, null);
        expect(cursor).not.toBeNull();
        expect(cursor.moveToPosition(position)).toBeTrue();
        expect(cursor).toHaveColumn(columnName, columnValue);
        cursor.close();
        return true;
    }
}
