package com.soundcloud.android.framework.helpers;

import static com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey.CHROMECAST;
import static com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey.PLAY_QUEUE;

import com.soundcloud.android.analytics.AnalyticsProviderFactory;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.configuration.FeatureName;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.configuration.PlanStorage;
import com.soundcloud.android.configuration.UserPlan.Upsell;
import com.soundcloud.android.configuration.features.Feature;
import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations;
import com.soundcloud.android.facebookinvites.FacebookInvitesStorage;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ObfuscatedPreferences;
import com.soundcloud.java.collections.Sets;
import rx.functions.Action1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.List;

@SuppressLint("CommitPrefEdits")
public class ConfigurationHelper {

    private static final String PREFS_FEATURES_SETTINGS = "features_settings";
    private static final String PREFS_POLICY_SETTINGS = "policy_settings";
    private static final String PREFS_ANALYTICS_SETTINGS = "analytics_settings";
    private static final String PREFS_OFFLINE_SETTINGS = "offline_settings";
    private static final String PREFS_FACEBOOK_INVITES_SETTINGS = "facebook_invites";
    private static final String PREFS_STATIONS = "stations";
    private static final String PREFS_INTRODUCTORY_OVERLAYS = "intro_overlays";

    private static final String ONBOARDING_LIKED_STATIONS_DISABLED = "ONBOARDING_LIKED_STATIONS_DISABLED";
    private static final String LAST_POLICY_CHECK_TIME = "last_policy_check_time";
    private static final String TAG = "TestRunner";

    private static final Upsell HIGH_TIER = new Upsell(Plan.HIGH_TIER.planId, 0);

    public static void enableOfflineContent(Context context) {
        enableFeature(context, FeatureName.OFFLINE_SYNC);
    }

    public static void enableUpsell(final Context context) {
        final PlanStorage planStorage = getPlanStorage(context);
        planStorage.updateUpsells(Collections.singletonList(HIGH_TIER));
        disableFeature(context, FeatureName.OFFLINE_SYNC);
        disableFeature(context, FeatureName.REMOVE_AUDIO_ADS);

        planStorage.getUpsellUpdates()
                   .doOnNext(plans -> {
                       if (!planStorage.getUpsells().contains(Plan.HIGH_TIER)) {
                           planStorage.updateUpsells(Collections.singletonList(HIGH_TIER));
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

    public static void forcePendingPlanDowngrade(Context context) {
        final SharedPreferences configSettings =
                context.getSharedPreferences("device_config_settings", Context.MODE_PRIVATE);
        configSettings.edit().putString("pending_plan_downgrade", Plan.FREE_TIER.planId).apply();
    }

    private static void enableFeature(Context context, final String name) {
        final Feature feature = new Feature(name, true, Collections.<String>emptyList());
        final FeatureStorage featureStorage = getFeatureStorage(context);

        Log.d(TAG, "updating feature manually: " + feature.name);
        featureStorage.update(feature);

        featureStorage.getUpdates(name)
                      .filter(RxUtils.IS_FALSE)
                      .doOnNext(enabled -> {
                          Log.d(TAG, "updating feature after change: " + feature.name);
                          featureStorage.update(feature);
                      })
                      .subscribe();
    }

    private static void disableFeature(Context context, final String name) {
        final Feature feature = new Feature(name, false, Collections.singletonList(Plan.HIGH_TIER.planId));
        final FeatureStorage featureStorage = getFeatureStorage(context);

        featureStorage.update(feature);

        featureStorage.getUpdates(name)
                      .filter(RxUtils.IS_TRUE)
                      .doOnNext(enabled -> featureStorage.update(feature))
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
        final SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_FEATURES_SETTINGS,
                                                                                 Context.MODE_PRIVATE);
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
        return context.getSharedPreferences(PREFS_ANALYTICS_SETTINGS, Context.MODE_PRIVATE);
    }

    private static SharedPreferences getStationsSettingsPreferences(Context context) {
        return context.getSharedPreferences(PREFS_STATIONS, Context.MODE_PRIVATE);
    }

    private static SharedPreferences getIntroductoryOverlayPreferences(Context context) {
        return context.getSharedPreferences(PREFS_INTRODUCTORY_OVERLAYS, Context.MODE_PRIVATE);
    }

    public static void resetPolicyCheckTime(Context context) {
        getPolicySettingsPreferences(context)
                .edit()
                .remove(LAST_POLICY_CHECK_TIME)
                .commit();
    }

    public static void setPolicyCheckTime(Context context, long time) {
        getPolicySettingsPreferences(context)
                .edit()
                .putLong(LAST_POLICY_CHECK_TIME, time)
                .commit();
    }

    public static void disablePromotedAnalytics(Context context) {
        Log.i("asdf", "Disable promoted analytics");
        getAnalyticsSettingsPreferences(context)
                .edit()
                .putStringSet(AnalyticsProviderFactory.DISABLED_PROVIDERS,
                              Sets.newHashSet(PromotedAnalyticsProvider.class.getName()))
                .commit();
    }

    public static void disableOfflineSettingsOnboarding(Context context) {
        setOfflineSettingsOnboarding(context, true);
    }

    public static void enableOfflineSettingsOnboarding(Context context) {
        setOfflineSettingsOnboarding(context, false);
    }

    public static void disableStationsOnboarding(Context context) {
        getStationsSettingsPreferences(context)
                .edit()
                .putBoolean(ONBOARDING_LIKED_STATIONS_DISABLED, true)
                .apply();
    }

    private static void setOfflineSettingsOnboarding(Context context, boolean value) {
        getOfflineSettingsPreferences(context)
                .edit()
                .putBoolean(OfflineSettingsStorage.OFFLINE_SETTINGS_ONBOARDING, value)
                .commit();
    }

    public static void disableIntroductoryOverlays(Context context) {
        SharedPreferences.Editor editor = getIntroductoryOverlayPreferences(context).edit();
        for (String key : IntroductoryOverlayKey.ALL_KEYS) {
            editor.putBoolean(key, true);
        }
        editor.apply();
    }
}
