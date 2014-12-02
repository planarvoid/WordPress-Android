package com.soundcloud.android.tests.search;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.AddToPlaylistsScreen;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class ItemOverflowTest extends ActivityTest<MainActivity> {

    private PlaylistTagsScreen playlistTagsScreen;

    public ItemOverflowTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());

        super.setUp();

        playlistTagsScreen = new MainScreen(solo)
                .actionBar()
                .clickSearchButton();
    }

    public void testClickingAddToPlaylistOverflowMenuItemOpensDialog() {
        playlistTagsScreen.actionBar()
                .doSearch("clownstep")
                .touchTracksTab()
                .clickFirstTrackOverflowButton()
                .clickAdToPlaylist();

        final AddToPlaylistsScreen addToPlaylistsScreen = new AddToPlaylistsScreen(solo);
        assertThat(addToPlaylistsScreen, is(visible()));
    }
}
