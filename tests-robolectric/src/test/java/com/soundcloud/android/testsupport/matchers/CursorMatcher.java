package com.soundcloud.android.testsupport.matchers;

import com.pivotallabs.greatexpectations.matchers.ObjectMatcher;

import android.database.Cursor;

public class CursorMatcher<T extends Cursor, M extends CursorMatcher<T, M>> extends ObjectMatcher<T, M> {

    public boolean toHaveCount(int expected) {
        if (actual.getCount() != expected) {
            failureMessage = actual + " to have count <" + expected + "> (is " + actual.getCount() + ")";
            return false;
        } else {
            return true;
        }
    }

    public boolean toHaveColumn(String name, int value) {
        int idx = checkColumn(name);
        if (idx < 0) {
            return false;
        }

        int i = actual.getInt(idx);
        if (i != value) {
            failureMessage = "column " + name + ": " + value + "!=" + i;
            return false;
        } else {
            return true;
        }
    }

    public boolean toHaveColumn(String name, long value) {
        int idx = checkColumn(name);
        if (idx < 0) {
            return false;
        }

        long i = actual.getLong(idx);
        if (i != value) {
            failureMessage = "column " + name + ": " + value + "!=" + i;
            return false;
        } else {
            return true;
        }
    }

    private int checkColumn(String name) {
        int idx = actual.getColumnIndex(name);
        if (idx < 0) {
            failureMessage = "there is no column called " + name + " for " + actual;
        }
        return idx;
    }
}
