package com.soundcloud.android.collection;

import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.storage.StorageModule;

import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;

public class CollectionOptionsStorage {
    private static final String ONBOARDING_DISABLED = "ONBOARDING_DISABLED";
    private static final String UPSELL_DISABLED = "UPSELL_DISABLED";

    @VisibleForTesting
    static final String KEY_SHOW_LIKES = "showLikes";
    static final String KEY_SHOW_POSTS = "showPosts";
    static final String KEY_SHOW_OFFLINE_ONLY = "showOfflineOnly";
    static final String KEY_SORT_BY_TITLE = "sortByTitle";

    private final SharedPreferences preferences;

    @Inject
    public CollectionOptionsStorage(@Named(StorageModule.COLLECTIONS) SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public void clear() {
        preferences.edit().clear().apply();
    }

    boolean isUpsellEnabled() {
        return !preferences.getBoolean(UPSELL_DISABLED, false);
    }

    public void disableUpsell() {
        preferences.edit().putBoolean(UPSELL_DISABLED, true).apply();
    }

    boolean isOnboardingEnabled() {
        return !preferences.getBoolean(ONBOARDING_DISABLED, false);
    }

    void disableOnboarding() {
        preferences.edit().putBoolean(ONBOARDING_DISABLED, true).apply();
    }

    public PlaylistsOptions getLastOrDefault() {
        return PlaylistsOptions.builder()
                               .showLikes(preferences.getBoolean(KEY_SHOW_LIKES, false))
                               .showPosts(preferences.getBoolean(KEY_SHOW_POSTS, false))
                               .showOfflineOnly(preferences.getBoolean(KEY_SHOW_OFFLINE_ONLY, false))
                               .sortByTitle(preferences.getBoolean(KEY_SORT_BY_TITLE, false))
                               .build();
    }

    public void store(PlaylistsOptions options) {
        preferences.edit()
                   .putBoolean(KEY_SHOW_LIKES, options.showLikes())
                   .putBoolean(KEY_SHOW_POSTS, options.showPosts())
                   .putBoolean(KEY_SHOW_OFFLINE_ONLY, options.showOfflineOnly())
                   .putBoolean(KEY_SORT_BY_TITLE, options.sortByTitle())
                   .apply();
    }

}
