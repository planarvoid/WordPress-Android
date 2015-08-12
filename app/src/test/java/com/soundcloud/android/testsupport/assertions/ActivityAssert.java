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

    private final Intent shadowIntent;

    public ActivityAssert(Activity actual) {
        super(actual, ActivityAssert.class);
        this.shadowIntent = Shadows.shadowOf(actual).getNextStartedActivity();
    }

    public IntentAssert nextStartedIntent() {
        isNotNull();
        Assertions.assertThat(shadowIntent).isNotNull();
        return new IntentAssert(shadowIntent);
    }

    public void hasNoNextStartedIntent() {
        isNotNull();
        Assertions.assertThat(shadowIntent).isNull();
    }
}
