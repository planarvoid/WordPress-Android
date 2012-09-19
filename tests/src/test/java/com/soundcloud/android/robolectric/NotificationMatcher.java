package com.soundcloud.android.robolectric;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.pivotallabs.greatexpectations.matchers.ObjectMatcher;
import com.xtremelabs.robolectric.Robolectric;

import android.app.Notification;
import android.content.Intent;

public class NotificationMatcher<T extends Notification, M extends NotificationMatcher<T, M>> extends ObjectMatcher<T, M> {
    public boolean toHaveTitle(CharSequence title) {
        descriptionOfActual = shadowOf(actual).getLatestEventInfo().getContentTitle().toString();
        return descriptionOfActual.equals(title);
    }

    public boolean toHaveText(CharSequence text) {
        descriptionOfActual = shadowOf(actual).getLatestEventInfo().getContentText().toString();
        return descriptionOfActual.equals(text);
    }

    public boolean toHaveTicker(CharSequence text) {
        descriptionOfActual = text.toString();
        return descriptionOfActual.equals(text);
    }

    public boolean toMatchIntent(Intent intent) {
        Intent saved = shadowOf(shadowOf(actual).getLatestEventInfo().getContentIntent()).getSavedIntent();
        descriptionOfActual = saved.toString();
        descriptionOfExpected = intent.toString();
        return saved.filterEquals(intent);
    }
}
