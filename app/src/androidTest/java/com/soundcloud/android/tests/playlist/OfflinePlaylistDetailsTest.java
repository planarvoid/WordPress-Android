package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

public class OfflinePlaylistDetailsTest extends ActivityTest<MainActivity> {

    public OfflinePlaylistDetailsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.offlineUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        final Context context = getInstrumentation().getTargetContext();
        resetOfflineSyncState(context);
        enableOfflineContent(context);
    }

    public void testDownloadPlaylistWhenMadeAvailableOffline() {
        final PlaylistDetailsScreen playlistDetailsScreen = menuScreen.open()
                .clickPlaylists()
                .get(0)
                .click()
                .clickPlaylistOverflowButton()
                .clickMakeAvailableOffline();

        final DownloadImageViewElement downloadElement = playlistDetailsScreen.headerDownloadElement();
        assertTrue("Playlist should be requested or downloading ",
                downloadElement.isRequested() || downloadElement.isDownloading() || downloadElement.isDownloaded());
    }
}
