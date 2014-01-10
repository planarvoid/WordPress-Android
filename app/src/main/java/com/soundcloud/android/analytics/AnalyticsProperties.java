package com.soundcloud.android.analytics;


import static com.google.common.base.Preconditions.checkArgument;
import static com.soundcloud.android.utils.ScTextUtils.isNotBlank;

import com.google.common.base.Objects;
import com.soundcloud.android.R;
import com.soundcloud.android.preferences.SettingsActivity;

import android.content.SharedPreferences;
import android.content.res.Resources;

public class AnalyticsProperties implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final String mLocalyticsAppKey;
    private boolean mAnalyticsEnabledForBuild, mAnalyticsEnabledViaSettings;

    public AnalyticsProperties(Resources resources, SharedPreferences preferences) {
        mAnalyticsEnabledForBuild = resources.getBoolean(R.bool.analytics_enabled);
        mLocalyticsAppKey = resources.getString(R.string.localytics_app_key);
        checkArgument(isNotBlank(mLocalyticsAppKey), "Localytics keys must be provided");

        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    public String getLocalyticsAppKey() {
        return mLocalyticsAppKey;
    }

    public boolean isAnalyticsDisabled() {
        return !isAnalyticsEnabled();
    }

    public boolean isAnalyticsEnabled() {
        return mAnalyticsEnabledForBuild && mAnalyticsEnabledViaSettings;
    }

    @Override
    public String toString(){
        return Objects.toStringHelper(this).add("analyticsEnabled", mAnalyticsEnabledForBuild).toString();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingsActivity.ANALYTICS_ENABLED.equalsIgnoreCase(key)) {
            mAnalyticsEnabledViaSettings = sharedPreferences.getBoolean(SettingsActivity.ANALYTICS_ENABLED, true);
        }
    }
}
