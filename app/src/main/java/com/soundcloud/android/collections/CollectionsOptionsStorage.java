package com.soundcloud.android.collections;

import com.soundcloud.android.storage.StorageModule;

import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;

class CollectionsOptionsStorage {
    private static final String ONBOARDING_DISABLED = "ONBOARDING_DISABLED";

    @VisibleForTesting
    static final String KEY_SHOW_LIKES = "showLikes";
    static final String KEY_SHOW_POSTS = "showPosts";
    static final String KEY_SHOW_OFFLINE_ONLY = "showOfflineOnly";
    static final String KEY_SORT_BY_TITLE = "sortByTitle";

    private final SharedPreferences preferences;

    @Inject
    public CollectionsOptionsStorage(@Named(StorageModule.COLLECTIONS) SharedPreferences preferences) {
        this.preferences = preferences;
    }

    void clear() {
        preferences.edit().clear().apply();
    }

    boolean isOnboardingEnabled() {
        return !preferences.getBoolean(ONBOARDING_DISABLED, false);
    }

    void disableOnboarding() {
        preferences.edit().putBoolean(ONBOARDING_DISABLED, true).apply();
    }


    public CollectionsOptions getLastOrDefault() {
        return CollectionsOptions.builder()
                .showLikes(preferences.getBoolean(KEY_SHOW_LIKES, false))
                .showPosts(preferences.getBoolean(KEY_SHOW_POSTS, false))
                .showOfflineOnly(preferences.getBoolean(KEY_SHOW_OFFLINE_ONLY, false))
                .sortByTitle(preferences.getBoolean(KEY_SORT_BY_TITLE, false))
                .build();
    }

    public void store(CollectionsOptions options) {
        preferences.edit()
                .putBoolean(KEY_SHOW_LIKES, options.showLikes())
                .putBoolean(KEY_SHOW_POSTS, options.showPosts())
                .putBoolean(KEY_SHOW_OFFLINE_ONLY, options.showOfflineOnly())
                .putBoolean(KEY_SORT_BY_TITLE, options.sortByTitle())
                .apply();
    }

}
