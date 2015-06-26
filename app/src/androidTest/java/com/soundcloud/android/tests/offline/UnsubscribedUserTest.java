package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.ActivityTest;

public class UnsubscribedUserTest extends ActivityTest<MainActivity> {

    public UnsubscribedUserTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.likesUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testDownloadIsUnavailableWhenTheyAccessLikes() throws Exception {
        final ViewElement overflowButton = menuScreen
                .open()
                .clickLikes()
                .headerOverflowButton();

        assertThat(overflowButton, is(not(visible())));
    }

    public void testDownloadIsUnavailableWhenTheyAccessPlaylists() throws Exception {
        final ViewElement offlineItem = menuScreen
                .open()
                .clickPlaylists()
                .getPlaylists()
                .get(0)
                .clickOverflow()
                .getMakeAvailableOfflineItem();

        assertThat(offlineItem, is(not(visible())));
    }

    public void testDownloadIsUnavailableWhenTheyAccessLikedPlaylists() throws Exception {
        final ViewElement offlineItem = menuScreen
                .open()
                .clickPlaylists()
                .touchLikedPlaylistsTab()
                .getPlaylists()
                .get(0)
                .clickOverflow()
                .getMakeAvailableOfflineItem();

        assertThat(offlineItem, is(not(visible())));
    }

    public void testDownloadIsUnavailableWhenTheyAccessPlaylistDetailScreen() throws Exception {
        final ViewElement offlineItem = menuScreen
                .open()
                .clickPlaylists()
                .clickPlaylistAt(0)
                .clickPlaylistOverflowButton()
                .getMakeAvailableOfflineItem();

        assertThat(offlineItem, is(not(visible())));
    }
}
