package com.soundcloud.android.search.suggestions;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.test.IntegrationTest;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class ShortcutsStorageTest extends StorageIntegrationTest {

    private ShortcutsStorage shortcutsStorage;

    @Before
    public void setUp() throws Exception {
        shortcutsStorage = new ShortcutsStorage(propeller());
    }

    @Test
    public void returnsTrackLikeFromStorageMatchedOnFirstWord() {
        final ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date());

        final List<Shortcut> shortcuts = shortcutsStorage.getShortcuts(apiTrack.getTitle().substring(0, 3), 1);

        assertThat(shortcuts).containsExactly(Shortcut.create(apiTrack.getUrn(), apiTrack.getTitle()));
    }

    @Test
    public void returnsMatchedTrackLikeFromStorageMatchedOnSecondWord() {
        final ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date());

        final String title = apiTrack.getTitle();
        final int startIndex = title.indexOf(" ");
        final List<Shortcut> shortcuts = shortcutsStorage.getShortcuts(title.substring(startIndex + 1, startIndex + 3), 1);

        assertThat(shortcuts).containsExactly(Shortcut.create(apiTrack.getUrn(), title));
    }

    @Test
    public void returnsMatchedPlaylistLikeFromStorage() {
        final ApiPlaylist apiPlaylist = testFixtures().insertLikedPlaylist(new Date());

        final List<Shortcut> shortcuts = shortcutsStorage.getShortcuts(apiPlaylist.getTitle().substring(0, 3), 1);

        assertThat(shortcuts).containsExactly(Shortcut.create(apiPlaylist.getUrn(), apiPlaylist.getTitle()));
    }

    @Test
    public void returnsMatchedFollowingFromStorage() {
        final ApiUser apiUser = testFixtures().insertUser();
        testFixtures().insertFollowing(apiUser.getUrn());

        final List<Shortcut> shortcuts = shortcutsStorage.getShortcuts(apiUser.getUsername().substring(0, 3), 1);

        assertThat(shortcuts).containsExactly(Shortcut.create(apiUser.getUrn(), apiUser.getUsername()));
    }

    @Test
    public void returnsAllTypesFromStorage() {
        final ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date());
        final ApiPlaylist apiPlaylist = testFixtures().insertLikedPlaylist(new Date());
        final ApiUser apiUser = testFixtures().insertUser();
        testFixtures().insertFollowing(apiUser.getUrn());

        final List<Shortcut> shortcuts = shortcutsStorage.getShortcuts("", 3);

        assertThat(shortcuts).contains(
                Shortcut.create(apiUser.getUrn(), apiUser.getUsername()),
                Shortcut.create(apiTrack.getUrn(), apiTrack.getTitle()),
                Shortcut.create(apiPlaylist.getUrn(), apiPlaylist.getTitle())
        );
    }

    @Test
    public void returnsLimitedItemsFromStorage() {
        final ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date());
        final ApiPlaylist apiPlaylist = testFixtures().insertLikedPlaylist(new Date());
        final ApiUser apiUser = testFixtures().insertUser();
        testFixtures().insertFollowing(apiUser.getUrn());

        final List<Shortcut> shortcuts = shortcutsStorage.getShortcuts("", 1);

        assertThat(shortcuts).containsExactly(
                Shortcut.create(apiUser.getUrn(), apiUser.getUsername())
        );
    }

    @Test
    public void returnsOnlyMatchedItemFromStorage() {
        final ApiTrack apiTrack = testFixtures().insertLikedTrack(new Date());
        final ApiPlaylist apiPlaylist = testFixtures().insertLikedPlaylist(new Date());
        final ApiUser apiUser = testFixtures().insertUser();
        testFixtures().insertFollowing(apiUser.getUrn());

        final List<Shortcut> shortcuts = shortcutsStorage.getShortcuts(apiUser.getUsername(), 3);

        assertThat(shortcuts).containsExactly(
                Shortcut.create(apiUser.getUrn(), apiUser.getUsername())
        );
    }
}
