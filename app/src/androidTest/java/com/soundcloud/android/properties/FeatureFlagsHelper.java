package com.soundcloud.android.properties;

import static com.soundcloud.android.storage.StorageModule.PREFS_FEATURE_FLAGS;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.storage.PersistentStorage;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.android.utils.ObfuscatedPreferences;
import com.soundcloud.annotations.VisibleForTesting;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

/**
 * {@link FeatureFlags} decorator to avoid increasing scope and visibility of
 * {@link FeatureFlags} class for testing purpose. <br>
 *
 * Used only for testing, otherwise use injectable {@link FeatureFlags} class.
 */
@VisibleForTesting
public class FeatureFlagsHelper {

    private final FeatureFlags featureFlags;

    private FeatureFlagsHelper(Context context) {
        final CurrentDateProvider currentDateProvider = new CurrentDateProvider();
        final GooglePlayServicesWrapper googlePlayServicesWrapper = new GooglePlayServicesWrapper();
        final PersistentStorage persistentStorage = new PersistentStorage(createPreferencesForFeatureFlags(context));
        final ApplicationProperties applicationProperties = new ApplicationProperties(context.getResources());
        final RemoteConfig remoteConfig = new RemoteConfig(createFirebaseRemoteConfig(applicationProperties), persistentStorage, currentDateProvider, googlePlayServicesWrapper);
        final LocalConfig localConfig = new LocalConfig();
        final RuntimeConfig runtimeConfig = new RuntimeConfig(persistentStorage);
        this.featureFlags = new FeatureFlags(remoteConfig, localConfig, runtimeConfig);
    }

    public static FeatureFlagsHelper create(Context context) {
        return new FeatureFlagsHelper(context);
    }

    public boolean isEnabled(Flag flag) {
        return featureFlags.isEnabled(flag);
    }

    public boolean isDisabled(Flag flag) {
        return featureFlags.isDisabled(flag);
    }

    public void enable(Flag flag) {
        featureFlags.setRuntimeFeatureFlagValue(flag, true);
    }

    public void disable(Flag flag) {
        featureFlags.setRuntimeFeatureFlagValue(flag, false);
    }

    @NonNull
    private SharedPreferences createPreferencesForFeatureFlags(Context context) {
        return new ObfuscatedPreferences(context.getSharedPreferences(PREFS_FEATURE_FLAGS, Context.MODE_PRIVATE), new Obfuscator());
    }

    @NonNull
    private FirebaseRemoteConfig createFirebaseRemoteConfig(ApplicationProperties appProperties) {
        final FirebaseRemoteConfigSettings remoteConfigSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(appProperties.isDevelopmentMode())
                .build();
        final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.setConfigSettings(remoteConfigSettings);
        return remoteConfig;
    }
}
