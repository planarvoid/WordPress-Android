package com.soundcloud.android.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.SharedPreferences;

@RunWith(MockitoJUnitRunner.class)
public class CollectionsOptionsStorageTest {

    private CollectionsOptionsStorage storage;

    @Mock private SharedPreferences preferences;
    @Mock private SharedPreferences.Editor preferencesEditor;

    @Before
    public void setUp() throws Exception {
        storage = new CollectionsOptionsStorage(preferences);
    }

    @Test
    public void loadsOptionsFromStorage() {
        when(preferences.getBoolean(CollectionsOptionsStorage.KEY_SHOW_POSTS, false)).thenReturn(true);
        when(preferences.getBoolean(CollectionsOptionsStorage.KEY_SHOW_LIKES, false)).thenReturn(true);
        when(preferences.getBoolean(CollectionsOptionsStorage.KEY_SHOW_OFFLINE_ONLY, false)).thenReturn(true);
        when(preferences.getBoolean(CollectionsOptionsStorage.KEY_SORT_BY_TITLE, false)).thenReturn(true);

        final CollectionsOptions lastOrDefault = storage.getLastOrDefault();

        assertThat(lastOrDefault.showPosts()).isTrue();
        assertThat(lastOrDefault.showLikes()).isTrue();
        assertThat(lastOrDefault.showOfflineOnly()).isTrue();
        assertThat(lastOrDefault.sortByTitle()).isTrue();
    }

    @Test
    public void storesOptionsInStorage() {
        when(preferences.edit()).thenReturn(preferencesEditor);
        when(preferencesEditor.putBoolean(anyString(), anyBoolean())).thenReturn(preferencesEditor);

        storage.store(getAllTrueOptions());

        verify(preferencesEditor).putBoolean(CollectionsOptionsStorage.KEY_SHOW_POSTS, true);
        verify(preferencesEditor).putBoolean(CollectionsOptionsStorage.KEY_SHOW_LIKES, true);
        verify(preferencesEditor).putBoolean(CollectionsOptionsStorage.KEY_SHOW_OFFLINE_ONLY, true);
        verify(preferencesEditor).putBoolean(CollectionsOptionsStorage.KEY_SORT_BY_TITLE, true);
        verify(preferencesEditor).apply();

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
