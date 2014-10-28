package com.soundcloud.android.analytics.adjust;

import com.adjust.sdk.Adjust;

import android.content.Context;

import javax.inject.Inject;

public class AdjustWrapper {
    private final Context applicationContext;

    @Inject
    public AdjustWrapper(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    /* package */ void onResume() {
        Adjust.onResume(applicationContext);
    }

    /* package */ void onPause() {
        Adjust.onPause();
    }
}
