package com.soundcloud.android.analytics;


import static com.google.common.base.Preconditions.checkArgument;
import static com.soundcloud.android.utils.ScTextUtils.isNotBlank;

import com.google.common.base.Objects;
import com.soundcloud.android.R;

import android.content.res.Resources;

import javax.inject.Inject;

public class AnalyticsProperties {
    private final String mLocalyticsAppKey;
    private final boolean mAnalyticsAvailable;

    @Inject
    public AnalyticsProperties(Resources resources) {
        mAnalyticsAvailable = resources.getBoolean(R.bool.analytics_enabled);
        mLocalyticsAppKey = resources.getString(R.string.localytics_app_key);
        checkArgument(isNotBlank(mLocalyticsAppKey), "Localytics keys must be provided");
    }

    public String getLocalyticsAppKey() {
        return mLocalyticsAppKey;
    }

    public boolean isAnalyticsAvailable() {
        return mAnalyticsAvailable;
    }

    @Override
    public String toString(){
        return Objects.toStringHelper(this).add("analyticsEnabled", mAnalyticsAvailable).toString();
    }
}
