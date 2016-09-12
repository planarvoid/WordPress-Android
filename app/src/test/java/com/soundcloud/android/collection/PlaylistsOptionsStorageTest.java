package com.soundcloud.android.collection;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.content.SharedPreferences;

public class PlaylistsOptionsStorageTest extends AndroidUnitTest {

    private CollectionOptionsStorage storage;

    private SharedPreferences preferences;

    @Before
    public void setUp() throws Exception {
        preferences = sharedPreferences();
        storage = new CollectionOptionsStorage(preferences);
    }

    @Test
    public void loadsOptionsFromStorage() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(CollectionOptionsStorage.KEY_SHOW_POSTS, true);
        editor.putBoolean(CollectionOptionsStorage.KEY_SHOW_LIKES, true);
        editor.putBoolean(CollectionOptionsStorage.KEY_SHOW_OFFLINE_ONLY, true);
        editor.putBoolean(CollectionOptionsStorage.KEY_SORT_BY_TITLE, true);
        editor.apply();

        final PlaylistsOptions lastOrDefault = storage.getLastOrDefault();

        assertThat(lastOrDefault.showPosts()).isTrue();
        assertThat(lastOrDefault.showLikes()).isTrue();
        assertThat(lastOrDefault.showOfflineOnly()).isTrue();
        assertThat(lastOrDefault.sortByTitle()).isTrue();
    }

    @Test
    public void storesOptionsInStorage() {
        storage.store(getAllTrueOptions());

        assertThat(preferences.getBoolean(CollectionOptionsStorage.KEY_SHOW_POSTS, false)).isTrue();
        assertThat(preferences.getBoolean(CollectionOptionsStorage.KEY_SHOW_LIKES, false)).isTrue();
        assertThat(preferences.getBoolean(CollectionOptionsStorage.KEY_SHOW_OFFLINE_ONLY, false)).isTrue();
        assertThat(preferences.getBoolean(CollectionOptionsStorage.KEY_SORT_BY_TITLE, false)).isTrue();
    }


    @Test
    public void isOnboardingEnabledReturnsTrueWhenStorageIsEmpty() {
        assertThat(storage.isOnboardingEnabled()).isTrue();
    }

    @Test
    public void isOnboardingEnabledReturnsFalseWhenHasBeenDisabled() {
        storage.disableOnboarding();
        assertThat(storage.isOnboardingEnabled()).isFalse();
    }

    @Test
    public void clearReset() {
        storage.disableOnboarding();

        storage.clear();

        assertThat(storage.isOnboardingEnabled()).isTrue();
    }

    private PlaylistsOptions getAllTrueOptions() {
        return PlaylistsOptions.builder()
                               .showLikes(true)
                               .showPosts(true)
                               .sortByTitle(true)
                               .showOfflineOnly(true)
                               .build();
    }
}
