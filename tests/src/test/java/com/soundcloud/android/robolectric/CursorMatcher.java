package com.soundcloud.android.robolectric;

import android.database.Cursor;
import com.pivotallabs.greatexpectations.matchers.ObjectMatcher;

public class CursorMatcher<T extends Cursor, M extends CursorMatcher<T, M>> extends ObjectMatcher<T, M> {

    public boolean toHaveCount(int expected) {
        if (actual.getCount() != expected) {
            failureMessage = actual + " to have count <" + expected + "> (is " +actual.getCount()+")";
            return false;
        } else {
            return true;
        }
    }

    public boolean toHaveColumn(String name) {
        return checkColumn(name) > 0;
    }

    public boolean toHaveColumn(String name, String value) {
        int idx = checkColumn(name);
        if (idx < 0) return false;

        String s = actual.getString(idx);

        if (!s.equals(value)) {
            failureMessage = "column "+name +": "+value +"!="+s;
            return false;
        } return true;
    }

    public boolean toHaveColumn(String name, int value) {
        int idx = checkColumn(name);
        if (idx < 0) return false;

        int i = actual.getInt(idx);
        if (i != value) {
            failureMessage = "column "+name +": "+value +"!="+i;
            return false;
        } else return true;
    }

    public boolean toHaveColumn(String name, long value) {
        int idx = checkColumn(name);
        if (idx < 0) return false;

        long i = actual.getLong(idx);
        if (i != value) {
            failureMessage = "column "+name +": "+value +"!="+i;
            return false;
        } else return true;
    }

    public boolean toHaveNext() {
        return actual.moveToNext();
    }

    private int checkColumn(String name) {
        int idx = actual.getColumnIndex(name);
        if (idx < 0) {
            failureMessage = "there is no column called "+name + " for "+actual;
        }
        return idx;
    }
}
