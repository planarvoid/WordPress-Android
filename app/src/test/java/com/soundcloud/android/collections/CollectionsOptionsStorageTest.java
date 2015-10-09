package com.soundcloud.android.collections;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.content.Context;
import android.content.SharedPreferences;

public class CollectionsOptionsStorageTest extends AndroidUnitTest {

    private CollectionsOptionsStorage storage;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    @Before
    public void setUp() throws Exception {
        preferences = sharedPreferences("test", Context.MODE_PRIVATE);
        storage = new CollectionsOptionsStorage(preferences);
    }

    @Test
    public void loadsOptionsFromStorage() {
        editor = preferences.edit();
        editor.putBoolean(CollectionsOptionsStorage.KEY_SHOW_POSTS, true);
        editor.putBoolean(CollectionsOptionsStorage.KEY_SHOW_LIKES, true);
        editor.putBoolean(CollectionsOptionsStorage.KEY_SHOW_OFFLINE_ONLY, true);
        editor.putBoolean(CollectionsOptionsStorage.KEY_SORT_BY_TITLE, true);
        editor.apply();

        final CollectionsOptions lastOrDefault = storage.getLastOrDefault();

        assertThat(lastOrDefault.showPosts()).isTrue();
        assertThat(lastOrDefault.showLikes()).isTrue();
        assertThat(lastOrDefault.showOfflineOnly()).isTrue();
        assertThat(lastOrDefault.sortByTitle()).isTrue();
    }

    @Test
    public void storesOptionsInStorage() {
        storage.store(getAllTrueOptions());

        assertThat(preferences.getBoolean(CollectionsOptionsStorage.KEY_SHOW_POSTS, false)).isTrue();
        assertThat(preferences.getBoolean(CollectionsOptionsStorage.KEY_SHOW_LIKES, false)).isTrue();
        assertThat(preferences.getBoolean(CollectionsOptionsStorage.KEY_SHOW_OFFLINE_ONLY, false)).isTrue();
        assertThat(preferences.getBoolean(CollectionsOptionsStorage.KEY_SORT_BY_TITLE, false)).isTrue();
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

    private CollectionsOptions getAllTrueOptions() {
        return CollectionsOptions.builder()
                    .showLikes(true)
                    .showPosts(true)
                    .sortByTitle(true)
                    .showOfflineOnly(true)
                    .build();
    }
}
