package com.soundcloud.android.stream;

import static com.soundcloud.android.tests.TestUser.streamUser;
import static com.soundcloud.android.tests.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.AddToPlaylistsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTestCase;

public class ItemOverflowTest extends ActivityTestCase<LauncherActivity> {

    private StreamScreen streamScreen;

    public ItemOverflowTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        setDependsOn(Feature.TRACK_ITEM_OVERFLOW);
        streamUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
    }

    public void testClickingAddToPlaylistOverflowMenuItemOpensDialog() {
        streamScreen = new StreamScreen(solo);
        streamScreen
                .clickFirstTrackOverflowButton()
                .clickAdToPlaylist();

        final AddToPlaylistsScreen addToPlaylistsScreen = new AddToPlaylistsScreen(solo);
        assertThat(addToPlaylistsScreen, is(visible()));
    }
}
