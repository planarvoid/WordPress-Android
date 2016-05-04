package com.soundcloud.android.tests.profile.legacy;

import static com.soundcloud.android.framework.TestUser.profileEntryUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.screens.elements.UserItemElement;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.tests.TestConsts;

import android.content.Intent;

public class OtherProfileTest extends ActivityTest<ResolveActivity> {

    private ProfileScreen profileScreen;

    public OtherProfileTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected void logInHelper() {
        profileEntryUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(TestConsts.OTHER_PROFILE_USER_URI));
        super.setUp();

        profileScreen = new ProfileScreen(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void testPostsLoadNextPage() {
        int postItemsBefore = profileScreen.currentItemCount();
        profileScreen.scrollToBottomAndLoadMoreItems();
        assertThat(postItemsBefore, is(lessThan(profileScreen.currentItemCount())));
    }

    public void testPostsTrackClickStartsPlayer() {
        assertThat(profileScreen.playTrack(0), is(visible()));
    }

    public void testPostsPlaylistClickOpensPlaylistPage() {
        final PlaylistElement expectedPlaylist = profileScreen
                .getPlaylists()
                .get(0);

        String targetPlaylistTitle = expectedPlaylist.getTitle();
        assertEquals(expectedPlaylist.click().getTitle(), targetPlaylistTitle);
    }

    public void testClickFollowingsLoadsProfile() {
        profileScreen.touchLegacyFollowingsTab();

        final UserItemElement expectedUser = profileScreen
                .getUsers()
                .get(0);
        waiter.waitForContentAndRetryIfLoadingFailed();

        String targetUsername = expectedUser.getUsername();
        assertEquals(expectedUser.click().getUserName(), targetUsername);
    }
}
