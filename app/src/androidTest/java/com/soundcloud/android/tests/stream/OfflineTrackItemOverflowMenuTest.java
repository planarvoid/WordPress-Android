package com.soundcloud.android.tests.stream;

import android.content.Context;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.CreatePlaylistScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTest;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class OfflineTrackItemOverflowMenuTest extends ActivityTest<MainActivity> {

    private StreamScreen streamScreen;

    public OfflineTrackItemOverflowMenuTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.streamUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {

        final Context context = getInstrumentation().getTargetContext();

        resetOfflineSyncState(context);
        super.setUp();

        menuScreen = new MenuScreen(solo);
        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();
        streamScreen = new StreamScreen(solo);
    }

    public void testWhenOfflineClickingAddToPlaylistOverflowMenuItemOpensDialog() {
        enableOfflineContent(getActivity());

        final CreatePlaylistScreen createPlaylistScreen = streamScreen.clickFirstTrackOverflowButton().
                clickAddToPlaylist().
                clickCreateNewPlaylist();

        assertThat(createPlaylistScreen, is(visible()));
        assertThat(createPlaylistScreen.offlineCheck().isVisible(), is(true));
    }

    private void resetOfflineSyncState(Context context) {
        ConfigurationHelper.disableOfflineSync(context);
        OfflineContentHelper.clearOfflineContent(context);
    }
}
