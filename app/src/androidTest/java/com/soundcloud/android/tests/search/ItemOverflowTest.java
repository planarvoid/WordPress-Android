package com.soundcloud.android.tests.search;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class ItemOverflowTest extends ActivityTest<MainActivity> {

    private PlaylistTagsScreen playlistTagsScreen;

    public ItemOverflowTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        playlistTagsScreen = new StreamScreen(solo)
                .actionBar()
                .clickSearchButton();
    }

    public void testClickingAddToPlaylistOverflowMenuItemOpensDialog() {
        playlistTagsScreen.actionBar()
                .doSearch("clownstep")
                .touchTracksTab()
                .clickFirstTrackOverflowButton()
                .clickAddToPlaylist();

        final AddToPlaylistScreen addToPlaylistScreen = new AddToPlaylistScreen(solo);
        assertThat(addToPlaylistScreen, is(visible()));
    }
}
