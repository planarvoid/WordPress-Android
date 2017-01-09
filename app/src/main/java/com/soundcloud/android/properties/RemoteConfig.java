package com.soundcloud.android.properties;

import static com.soundcloud.android.storage.StorageModule.FEATURES_FLAGS;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.soundcloud.android.storage.PersistentStorage;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.strings.Strings;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Singleton
class RemoteConfig {
    @VisibleForTesting
    static final long CACHE_EXPIRATION_TIME = TimeUnit.HOURS.toSeconds(2);
    @VisibleForTesting
    static final String REMOTE_FEATURE_FLAG_PREFIX = "android_feature_%s";

    private final FirebaseRemoteConfig firebaseRemoteConfig;
    private final PersistentStorage persistentStorage;
    private final CurrentDateProvider currentDateProvider;
    private final GooglePlayServicesWrapper googlePlayServicesWrapper;

    @Inject
    RemoteConfig(FirebaseRemoteConfig firebaseRemoteConfig, @Named(FEATURES_FLAGS) PersistentStorage persistentStorage,
                 CurrentDateProvider currentDateProvider, GooglePlayServicesWrapper googlePlayServicesWrapper) {
        this.firebaseRemoteConfig = firebaseRemoteConfig;
        this.persistentStorage = persistentStorage;
        this.currentDateProvider = currentDateProvider;
        this.googlePlayServicesWrapper = googlePlayServicesWrapper;
    }

    void fetchFeatureFlags(Context context) {
        if (isGooglePlayServicesAvailable(context) && shouldFetchRemoteConfig()) {
            firebaseRemoteConfig
                    .fetch(CACHE_EXPIRATION_TIME)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            firebaseRemoteConfig.activateFetched();
                            persistFeatureFlagValues();
                        } else {
                            ErrorUtils.handleSilentException(task.getException());
                        }
                    });
        }
    }

    private void persistFeatureFlagValues() {
        for (Flag featureFlag : Flag.features()) {
            final String featureFlagKey = getFlagKey(featureFlag);
            final String featureFlagValue = firebaseRemoteConfig.getString(featureFlagKey);
            if (Strings.isNotBlank(featureFlagValue)) {
                persistentStorage.persist(featureFlagKey, Boolean.valueOf(featureFlagValue));
            }
        }
    }

    private boolean shouldFetchRemoteConfig() {
        final int lastFetchStatus = firebaseRemoteConfig.getInfo().getLastFetchStatus();
        return lastFetchStatus == FirebaseRemoteConfig.LAST_FETCH_STATUS_FAILURE ||
                lastFetchStatus == FirebaseRemoteConfig.LAST_FETCH_STATUS_NO_FETCH_YET ||
                isCacheExpired();
    }

    private boolean isCacheExpired() {
        final long lastFetchTime = firebaseRemoteConfig.getInfo().getFetchTimeMillis();
        return (currentDateProvider.getCurrentTime() - lastFetchTime >= CACHE_EXPIRATION_TIME);
    }

    private boolean isGooglePlayServicesAvailable(Context context) {
        return googlePlayServicesWrapper.getPlayServicesAvailableStatus(context) == ConnectionResult.SUCCESS;
    }

    boolean getFlagValue(Flag flag, boolean defaultValue) {
        return persistentStorage.getValue(getFlagKey(flag), defaultValue);
    }

    String getFlagKey(Flag featureFlag) {
        return String.format(Locale.US, REMOTE_FEATURE_FLAG_PREFIX, featureFlag.featureName());
    }
}
