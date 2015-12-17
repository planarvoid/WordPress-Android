package com.soundcloud.android.tests.offline;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.BrokenScrollingTest;
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
        final ViewElement offlineItem = mainNavHelper.goToTrackLikes()
                .clickOverflowButton()
                .getMakeAvailableOfflineItem();

        assertThat(offlineItem, is(visible()));
    }

    @BrokenScrollingTest
    public void testDownloadIsAvailableWhenTheyAccessPlaylists() throws Exception {
        final ViewElement offlineItem = mainNavHelper.goToCollections()
                .scrollToFirstPlaylist()
                .clickOverflow()
                .getMakeAvailableOfflineItem();

        assertThat(offlineItem, is(visible()));
    }

    public void testDownloadIsAvailableWhenTheyAccessPlaylistDetailScreen() throws Exception {
        final ViewElement offlineItem = mainNavHelper.goToCollections()
                .clickOnFirstPlaylist()
                .clickPlaylistOverflowButton()
                .getMakeAvailableOfflineItem();

        assertThat(offlineItem, is(visible()));
    }
}
