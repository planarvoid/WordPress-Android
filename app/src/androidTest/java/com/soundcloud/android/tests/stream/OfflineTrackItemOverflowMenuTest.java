package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.BrokenOfflineTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CreatePlaylistScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

@BrokenOfflineTest
public class OfflineTrackItemOverflowMenuTest extends ActivityTest<MainActivity> {

    private Context context;
    private StreamScreen streamScreen;
    private final OfflineContentHelper offlineContentHelper;

    public OfflineTrackItemOverflowMenuTest() {
        super(MainActivity.class);
        offlineContentHelper = new OfflineContentHelper();
    }

    @Override
    protected void logInHelper() {
        TestUser.streamUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {

        context = getInstrumentation().getTargetContext();

        resetOfflineSyncState(context);
        super.setUp();

        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();
        streamScreen = new StreamScreen(solo);
    }

    public void testWhenOfflineClickingAddToPlaylistOverflowMenuItemOpensDialog() {
        enableOfflineContent(context);

        final CreatePlaylistScreen createPlaylistScreen = streamScreen.clickFirstTrackCardOverflowButton().
                clickAddToPlaylist().
                clickCreateNewPlaylist();

        assertThat(createPlaylistScreen, is(visible()));
        assertThat(createPlaylistScreen.offlineCheck().isVisible(), is(true));
    }

    private void resetOfflineSyncState(Context context) {
        ConfigurationHelper.disableOfflineContent(context);
        offlineContentHelper.clearOfflineContent(context);
    }
}
