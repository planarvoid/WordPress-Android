package com.soundcloud.android.testsupport.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.robolectric.Shadows;

import android.app.Activity;
import android.content.Intent;

/**
 * Custom assertion class for testing Android Activities.
 */
public final class ActivityAssert extends AbstractAssert<ActivityAssert, Activity> {

    public ActivityAssert(Activity actual) {
        super(actual, ActivityAssert.class);
    }

    public IntentAssert nextStartedIntent() {
        final Intent shadowIntent = getNextStartedActivity();
        isNotNull();
        Assertions.assertThat(shadowIntent).isNotNull();
        return new IntentAssert(shadowIntent);
    }

    public void hasNoNextStartedIntent() {
        isNotNull();
        Assertions.assertThat(getNextStartedActivity()).isNull();
    }

    public IntentAssert nextStartedService() {
        final Intent shadowIntent = getNextStartedService();
        isNotNull();
        Assertions.assertThat(shadowIntent).isNotNull();
        return new IntentAssert(shadowIntent);
    }

    private Intent getNextStartedActivity() {
        return Shadows.shadowOf(actual).getNextStartedActivity();
    }

    private Intent getNextStartedService() {
        return Shadows.shadowOf(actual).getNextStartedService();
    }
}
