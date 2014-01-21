package com.soundcloud.android.robolectric;

import com.pivotallabs.greatexpectations.matchers.ObjectMatcher;

import android.content.Intent;
import android.net.Uri;

public class IntentMatcher<T extends Intent, M extends IntentMatcher<T, M>> extends ObjectMatcher<T, M> {

    public boolean toHaveAction(String expected) {
        return actual.getAction().equals(expected);
    }

    public boolean toHaveData(Uri expected) {
        return actual.getData().equals(expected);
    }

    public boolean toHaveFlag(int flag) {
        return (actual.getFlags() & flag) != 0;
    }
}
