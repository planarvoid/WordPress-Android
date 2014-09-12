package com.soundcloud.android.testsupport.matchers;

import com.pivotallabs.greatexpectations.matchers.ObjectMatcher;
import com.xtremelabs.robolectric.Robolectric;

import android.widget.Toast;

public class ToastMatcher<T extends Toast, M extends ToastMatcher<T, M>> extends ObjectMatcher<T, M> {

    public boolean toHaveMessage(int resourceId) {
        return toHaveMessage(Robolectric.application.getString(resourceId));
    }

    public boolean toHaveMessage(String message) {
        return Robolectric.shadowOf(actual).getTextOfLatestToast().equals(message);
    }

}
