package com.soundcloud.android.properties;


import android.content.res.Resources;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScTextUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class AnalyticsProperties {
    private final String localyticsAppKey;
    private final boolean analyticsEnabled;

    public AnalyticsProperties(Resources resources) {
        analyticsEnabled = resources.getBoolean(R.bool.analytics_enabled);
        if (analyticsEnabled) {
            localyticsAppKey = resources.getString(R.string.localytics_app_key);
            checkArgument(ScTextUtils.isNotBlank(localyticsAppKey));
        } else {
            localyticsAppKey = ScTextUtils.EMPTY_STRING;
        }
    }

    public String getLocalyticsAppKey() {
        checkState(analyticsEnabled, "Analytics is not enabled");
        return localyticsAppKey;
    }

    public boolean isAnalyticsDisabled() {
        return !analyticsEnabled;
    }

}
