package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.ActivityTest;

public class SubscribedUserTest extends ActivityTest<MainActivity> {

    public SubscribedUserTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        enableOfflineContent(getInstrumentation().getTargetContext());
    }

    @Override
    protected void logInHelper() {
        TestUser.offlineUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testDownloadIsAvailableWhenTheyAccessLikes() throws Exception {
        final ViewElement offlineItem = menuScreen
                .open()
                .clickLikes()
                .clickListHeaderOverflowButton()
                .getMakeAvailableOfflineItem();

        assertThat(offlineItem, is(visible()));
    }

    public void testDownloadIsAvailableWhenTheyAccessPlaylists() throws Exception {
        final ViewElement offlineItem = menuScreen
                .open()
                .clickPlaylists()
                .getPlaylists()
                .get(0)
                .clickOverflow()
                .getMakeAvailableOfflineItem();

        assertThat(offlineItem, is(visible()));
    }

    public void testDownloadIsAvailableWhenTheyAccessLikedPlaylists() throws Exception {
        final ViewElement offlineItem = menuScreen
                .open()
                .clickPlaylists()
                .touchLikedPlaylistsTab()
                .getPlaylists()
                .get(0)
                .clickOverflow()
                .getMakeAvailableOfflineItem();

        assertThat(offlineItem, is(visible()));
    }
}
