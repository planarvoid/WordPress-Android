package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.resetOfflineSyncState;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.elements.DownloadImageViewElement;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

public class OfflinePlaylistPostsTest extends ActivityTest<MainActivity> {

    public OfflinePlaylistPostsTest() {
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

    public void testDownloadsPlaylistWhenMadeAvailableOffline() {
        final PlaylistElement firstPlaylist = mainNavHelper.goToCollections().scrollToFirstPlaylist();
        firstPlaylist.clickOverflow().clickMakeAvailableOffline();

        final DownloadImageViewElement downloadElement = firstPlaylist.downloadElement();

        assertTrue("Playlist should be requested or downloading",
                downloadElement.isRequested() || downloadElement.isDownloaded() || downloadElement.isDownloading());
    }
}
