package com.soundcloud.android.search;

import static com.soundcloud.android.tests.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.AddToPlaylistsScreen;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

public class ItemOverflowTest extends ActivityTestCase<MainActivity> {

    private PlaylistTagsScreen playlistTagsScreen;

    public ItemOverflowTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        setDependsOn(Feature.TRACK_ITEM_OVERFLOW);
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
