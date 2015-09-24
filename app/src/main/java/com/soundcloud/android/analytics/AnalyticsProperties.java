package com.soundcloud.android.analytics;


import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.R;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;

import javax.inject.Inject;

public class AnalyticsProperties {
    private final String localyticsAppKey;
    private final boolean analyticsAvailable;

    @Inject
    public AnalyticsProperties(Resources resources) {
        analyticsAvailable = resources.getBoolean(R.bool.analytics_enabled);
        localyticsAppKey = resources.getString(R.string.localytics_app_key);
        checkArgument(Strings.isNotBlank(localyticsAppKey), "Localytics keys must be provided");
    }

    public String getLocalyticsAppKey() {
        return localyticsAppKey;
    }

    public boolean isAnalyticsAvailable() {
        return analyticsAvailable;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("analyticsEnabled", analyticsAvailable).toString();
    }
}
