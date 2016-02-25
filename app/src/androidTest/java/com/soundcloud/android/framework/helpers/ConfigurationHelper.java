package com.soundcloud.android.framework.helpers;

import com.soundcloud.android.analytics.AnalyticsProviderFactory;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.configuration.FeatureName;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.configuration.PlanStorage;
import com.soundcloud.android.configuration.features.Feature;
import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations;
import com.soundcloud.android.facebookinvites.FacebookInvitesStorage;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ObfuscatedPreferences;
import com.soundcloud.java.collections.Sets;
import rx.functions.Action1;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.List;

public class ConfigurationHelper {

    private static final String PREFS_FEATURES_SETTINGS = "features_settings";
    private static final String PREFS_POLICY_SETTINGS = "policy_settings";
    private static final String PREFS_OFFLINE_SETTINGS = "offline_settings";
    private static final String PREFS_FACEBOOK_INVITES_SETTINGS = "facebook_invites";
    private static final String LAST_POLICY_CHECK_TIME = "last_policy_check_time";
    private static final String TAG = "TestRunner";

    public static void enableOfflineContent(Context context) {
        enableFeature(context, FeatureName.OFFLINE_SYNC);
    }

    public static void enableUpsell(final Context context) {
        final PlanStorage planStorage = getPlanStorage(context);
        planStorage.updateUpsells(Collections.singletonList(Plan.HIGH_TIER));
        disableFeature(context, FeatureName.OFFLINE_SYNC);
        disableFeature(context, FeatureName.REMOVE_AUDIO_ADS);

        planStorage.getUpsellUpdates()
                .doOnNext(new Action1<List<Plan>>() {
                    @Override
                    public void call(List<Plan> plans) {
                        if (!planStorage.getUpsells().contains(Plan.HIGH_TIER)) {
                            planStorage.updateUpsells(Collections.singletonList(Plan.HIGH_TIER));
                        }
                    }
                }).subscribe();
    }

    public static void disableOfflineContent(Context context) {
        disableFeature(context, FeatureName.OFFLINE_SYNC);
    }

    public static void resetOfflineSyncState(Context context) {
        new OfflineContentHelper().clearOfflineContent(context);
    }

    public static void forceFacebookListenerInvitesNotification(final Context context) {
        final FacebookInvitesStorage facebookInvitesStorage = getFacebookInvitesStorage(context);
        facebookInvitesStorage.setTimesAppOpened(FacebookInvitesOperations.SHOW_AFTER_OPENS_COUNT - 1);
        facebookInvitesStorage.setLastCreatorDismissMillisAgo(FacebookInvitesOperations.CREATOR_DISMISS_FOR_LISTENERS_INTERVAL_MS + 1);
        facebookInvitesStorage.setLastClick(0);
        facebookInvitesStorage.resetDismissed();
    }

    public static void disableFacebookInvitesNotification(final Context context) {
        final FacebookInvitesStorage facebookInvitesStorage = getFacebookInvitesStorage(context);
        facebookInvitesStorage.setTimesAppOpened(0);
    }

    private static void enableFeature(Context context, final String name) {
        final Feature feature = new Feature(name, true, Collections.<String>emptyList());
        final FeatureStorage featureStorage = getFeatureStorage(context);

        Log.d(TAG, "updating feature manually: " + feature.name);
        featureStorage.update(feature);

        featureStorage.getUpdates(name)
                .filter(RxUtils.IS_FALSE)
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean enabled) {
                        Log.d(TAG, "updating feature after change: " + feature.name);
                        featureStorage.update(feature);
                    }
                })
                .subscribe();
    }

    private static void disableFeature(Context context, final String name) {
        final Feature feature = new Feature(name, false, Collections.singletonList(Plan.HIGH_TIER.planId));
        final FeatureStorage featureStorage = getFeatureStorage(context);

        featureStorage.update(feature);

        featureStorage.getUpdates(name)
                .filter(RxUtils.IS_TRUE)
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean enabled) {
                        featureStorage.update(feature);
                    }
                })
                .subscribe();
    }

    private static FeatureStorage getFeatureStorage(Context context) {
        final SharedPreferences sharedPreferences = getFeaturesPreferences(context);
        return new FeatureStorage(new ObfuscatedPreferences(sharedPreferences, new Obfuscator()));
    }

    private static SharedPreferences getFeaturesPreferences(Context context) {
        return context.getSharedPreferences(PREFS_FEATURES_SETTINGS, Context.MODE_PRIVATE);
    }

    private static SharedPreferences getOfflineSettingsPreferences(Context context) {
        return context.getSharedPreferences(PREFS_OFFLINE_SETTINGS, Context.MODE_PRIVATE);
    }

    private static PlanStorage getPlanStorage(Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_FEATURES_SETTINGS, Context.MODE_PRIVATE);
        return new PlanStorage(new ObfuscatedPreferences(sharedPreferences, new Obfuscator()));
    }

    private static FacebookInvitesStorage getFacebookInvitesStorage(Context context) {
        final SharedPreferences sharedPreferences = getFacebookInvitesPreferences(context);
        return new FacebookInvitesStorage(sharedPreferences, new CurrentDateProvider());
    }

    private static SharedPreferences getFacebookInvitesPreferences(Context context) {
        return context.getSharedPreferences(PREFS_FACEBOOK_INVITES_SETTINGS, Context.MODE_PRIVATE);
    }

    private static SharedPreferences getPolicySettingsPreferences(Context context) {
        return context.getSharedPreferences(PREFS_POLICY_SETTINGS, Context.MODE_PRIVATE);
    }

    private static SharedPreferences getAnalyticsSettingsPreferences(Context context) {
        return context.getSharedPreferences(PREFS_POLICY_SETTINGS, Context.MODE_PRIVATE);
    }

    public static void resetPolicyCheckTime(Context context) {
        getPolicySettingsPreferences(context)
                .edit()
                .remove(LAST_POLICY_CHECK_TIME)
                .apply();
    }

    public static void setPolicyCheckTime(Context context, long time) {
        getPolicySettingsPreferences(context)
                .edit()
                .putLong(LAST_POLICY_CHECK_TIME, time)
                .commit();
    }

    public static void disablePromotedAnalytics(Context context) {
        getAnalyticsSettingsPreferences(context)
                .edit()
                .putStringSet(AnalyticsProviderFactory.DISABLED_PROVIDERS, Sets.newHashSet(PromotedAnalyticsProvider.class.getName()))
                .apply();
    }

    public static void disableOfflineSettingsOnboarding(Context context) {
        setOfflineSettingsOnboarding(context, true);
    }

    public static void enableOfflineSettingsOnboarding(Context context) {
        setOfflineSettingsOnboarding(context, false);
    }

    private static void setOfflineSettingsOnboarding(Context context, boolean value) {
        getOfflineSettingsPreferences(context)
                .edit()
                .putBoolean(OfflineSettingsStorage.OFFLINE_SETTINGS_ONBOARDING, value)
                .apply();
    }
}
