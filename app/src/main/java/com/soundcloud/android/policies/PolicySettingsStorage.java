package com.soundcloud.android.policies;

import com.soundcloud.android.Consts;
import com.soundcloud.android.storage.StorageModule;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

public class PolicySettingsStorage {
    private static final String LAST_POLICY_UPDATE_TIME = "last_policy_update_time";
    private static final String LAST_POLICY_CHECK_TIME = "last_policy_check_time";

    private final SharedPreferences sharedPreferences;

    @Inject
    public PolicySettingsStorage(@Named(StorageModule.POLICY_SETTINGS) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void setPolicyUpdateTime(long policiesCheckTime) {
        sharedPreferences.edit().putLong(LAST_POLICY_UPDATE_TIME, policiesCheckTime).apply();
    }

    public long getPolicyUpdateTime() {
        return sharedPreferences.getLong(LAST_POLICY_UPDATE_TIME, Consts.NOT_SET);
    }


    public void setLastPolicyCheckTime(long goBackOnlineShownTimestamp) {
        sharedPreferences.edit().putLong(LAST_POLICY_CHECK_TIME, goBackOnlineShownTimestamp).apply();
    }

    public long getLastPolicyCheckTime() {
        return sharedPreferences.getLong(LAST_POLICY_CHECK_TIME, Consts.NOT_SET);
    }
}
