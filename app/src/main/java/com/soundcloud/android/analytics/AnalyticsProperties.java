package com.soundcloud.android.analytics;


import static com.google.common.base.Preconditions.checkArgument;
import static com.soundcloud.android.utils.ScTextUtils.isNotBlank;

import com.google.common.base.Objects;
import com.soundcloud.android.R;

import android.content.res.Resources;

import javax.inject.Inject;

public class AnalyticsProperties {
    private final String localyticsAppKey;
    private final boolean analyticsAvailable;

    @Inject
    public AnalyticsProperties(Resources resources) {
        analyticsAvailable = resources.getBoolean(R.bool.analytics_enabled);
        localyticsAppKey = resources.getString(R.string.localytics_app_key);
        checkArgument(isNotBlank(localyticsAppKey), "Localytics keys must be provided");
    }

    public String getLocalyticsAppKey() {
        return localyticsAppKey;
    }

    public boolean isAnalyticsAvailable() {
        return analyticsAvailable;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("analyticsEnabled", analyticsAvailable).toString();
    }
}
