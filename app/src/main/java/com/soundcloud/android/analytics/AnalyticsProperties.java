package com.soundcloud.android.analytics;


import android.content.res.Resources;
import com.google.common.base.Objects;
import com.soundcloud.android.R;

import static com.google.common.base.Preconditions.checkArgument;
import static com.soundcloud.android.utils.ScTextUtils.isNotBlank;

public class AnalyticsProperties {
    private final String localyticsAppKey;
    private final boolean analyticsEnabled;

    public AnalyticsProperties(Resources resources) {
        analyticsEnabled = resources.getBoolean(R.bool.analytics_enabled);
        localyticsAppKey = resources.getString(R.string.localytics_app_key);
        checkArgument(isNotBlank(localyticsAppKey), "Localytics keys must be provided");
    }

    public String getLocalyticsAppKey() {
        return localyticsAppKey;
    }

    public boolean isAnalyticsDisabled() {
        return !analyticsEnabled;
    }

    public boolean isAnalyticsEnabled() {
        return analyticsEnabled;
    }

    @Override
    public String toString(){
        return Objects.toStringHelper(this).add("analyticsEnabled", analyticsEnabled).toString();
    }

}
