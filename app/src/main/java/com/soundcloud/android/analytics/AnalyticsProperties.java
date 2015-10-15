package com.soundcloud.android.analytics;

import com.soundcloud.android.R;
import com.soundcloud.java.objects.MoreObjects;

import android.content.res.Resources;

import javax.inject.Inject;

public class AnalyticsProperties {
    private final boolean analyticsAvailable;

    @Inject
    public AnalyticsProperties(Resources resources) {
        analyticsAvailable = resources.getBoolean(R.bool.analytics_enabled);
    }

    public boolean isAnalyticsAvailable() {
        return analyticsAvailable;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("analyticsEnabled", analyticsAvailable).toString();
    }
}
