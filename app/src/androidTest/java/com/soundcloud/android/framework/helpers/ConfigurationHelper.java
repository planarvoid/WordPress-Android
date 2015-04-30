package com.soundcloud.android.framework.helpers;

import static com.soundcloud.android.framework.helpers.OfflineContentHelper.clearOfflineContent;

import com.soundcloud.android.configuration.PlanStorage;
import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.crypto.Obfuscator;
import rx.functions.Action1;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigurationHelper {

    // Features
    private static final String PREFS_OFFLINE_SETTINGS = "offline_settings";
    private static final String OFFLINE_CONTENT = "offline_sync";
    private static final String LAST_POLICY_UPDATE_CHECK = "last_policy_update_check";

    // Plan
    private static final String PLAN_UPSELL = "upsell";
    private static final String PLAN_MID_TIER = "mid-tier";

    public static void enableOfflineContent(Context context) {
        enableFeature(context, OFFLINE_CONTENT);
    }

    public static void enableUpsell(Context context) {
        getPlanStorage(context).update(PLAN_UPSELL, PLAN_MID_TIER);
    }
    
    public static void disableOfflineContent(Context context) {
        getFeatureStorage(context).update(OFFLINE_CONTENT, false);
    }

    public static void resetOfflineSyncState(Context context) {
        disableOfflineContent(context);
        clearOfflineContent(context);
    }
    
    private static void enableFeature(Context context, final String feature) {
        final FeatureStorage featureStorage = getFeatureStorage(context);

        featureStorage.update(feature, true);

        featureStorage.getUpdates(feature)
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean enabled) {
                        if (!enabled) {
                            featureStorage.update(feature, true);
                        }
                    }
                })
                .subscribe();
    }

    private static FeatureStorage getFeatureStorage(Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("features_settings", Context.MODE_PRIVATE);
        return new FeatureStorage(sharedPreferences, new Obfuscator());
    }

    private static PlanStorage getPlanStorage(Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("features_settings", Context.MODE_PRIVATE);
        return new PlanStorage(sharedPreferences, new Obfuscator());
    }

    private static SharedPreferences getOfflineSettingsPreferences(Context context) {
        return context.getSharedPreferences(PREFS_OFFLINE_SETTINGS, Context.MODE_PRIVATE);
    }

    public static void resetPolicyUpdateCheckTime(Context context) {
        getOfflineSettingsPreferences(context)
                .edit()
                .remove(LAST_POLICY_UPDATE_CHECK)
                .commit();
    }

    public static void setPolicyUpdateCheckTime(Context context, long time) {
        getOfflineSettingsPreferences(context)
                .edit()
                .putLong(LAST_POLICY_UPDATE_CHECK, time)
                .commit();
    }

}
