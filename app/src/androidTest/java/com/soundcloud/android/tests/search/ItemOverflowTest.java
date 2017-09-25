package com.soundcloud.android.tests.search;

import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.discovery.SearchResultsTest.QUERY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class ItemOverflowTest extends ActivityTest<MainActivity> {

    public ItemOverflowTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Test
    public void testClickingAddToPlaylistOverflowMenuItemOpensDialog() throws Exception {
        mainNavHelper
                .goToDiscovery()
                .clickSearch()
                .doSearch(QUERY)
                .goToTracksTab()
                .clickFirstTrackOverflowButton()
                .clickAddToPlaylist();

        final AddToPlaylistScreen addToPlaylistScreen = new AddToPlaylistScreen(solo);
        assertThat(addToPlaylistScreen, is(visible()));
    }
}
