package com.soundcloud.android.testsupport;

import org.robolectric.Robolectric;
import org.robolectric.util.ActivityController;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class TestActivityController {

    private FragmentActivity activity;
    private ActivityController controller;

    private Bundle bundle = new Bundle();

    private TestActivityController(FragmentActivity activity) {
        this.activity = activity;
        this.controller = ActivityController.of(Robolectric.getShadowsAdapter(), activity);
        this.controller.withIntent(new Intent());
    }

    public static TestActivityController of(FragmentActivity activity) {
        return new TestActivityController(activity);
    }

    public void setIntent(Intent intent) {
        controller.withIntent(intent);
    }

    public void create() {
        create(bundle);
    }

    public void create(Bundle bundle) {
        controller.create(bundle);
    }

    public void finish() {
        activity.finish();
    }

    public FragmentActivity getActivity() {
        return activity;
    }
}
