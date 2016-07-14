package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.profileEntryUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.screens.elements.UserItemElement;
import com.soundcloud.android.tests.TestConsts;

import android.content.Intent;

public class OtherProfileTest extends TrackingActivityTest<ResolveActivity> {
    private String OTHER_PROFILE_PAGEVIEWS_SCENARIO = "other_profile_pageview_events";

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

    public void testPostsTrackClickStartsPlayer() {
        assertThat(profileScreen.playTrack(0), is(visible()));
    }

    public void testPostsPlaylistClickOpensPlaylistPage() {
        final PlaylistElement expectedPlaylist = profileScreen
                .scrollToPlaylists()
                .get(0);

        assertEquals(expectedPlaylist.click().getTitle(), expectedPlaylist.getTitle());
    }

    public void testClickFollowingsLoadsProfile() {
        profileScreen.touchFollowingsTab();

        final UserItemElement expectedUser = profileScreen
                .getUsers()
                .get(0);

        String targetUsername = expectedUser.getUsername();
        assertEquals(expectedUser.click().getUserName(), targetUsername);
    }

    public void testPageViewEvents() {
        startEventTracking();

        profileScreen.touchInfoTab();
        profileScreen.touchSoundsTab();
        profileScreen.touchFollowersTab();
        profileScreen.touchFollowingsTab();

        finishEventTracking(OTHER_PROFILE_PAGEVIEWS_SCENARIO);
    }
}
