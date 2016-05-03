package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.NewProfileTest;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;

@NewProfileTest
public class MyProfileTest extends ActivityTest<ResolveActivity> {

    private ProfileScreen profileScreen;

    public MyProfileTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected void logInHelper() { TestUser.profileTestUser.logIn(getInstrumentation().getTargetContext()); }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        profileScreen = mainNavHelper.goToMyProfile();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void testTrackClickStartsPlayer() {
        assertThat(profileScreen.playTrack(0), is(visible()));
    }

    public void testTracksViewAllOpensTracksPage() {
        profileScreen.clickViewAllTracks();
        assertEquals(profileScreen.getActionBarTitle(),
                    ressourceString(R.string.user_profile_sounds_header_tracks));
    }

    public void testPlaylistClickOpensPlaylistPage() {
        profileScreen.scrollToFirstPlaylist().click();
        assertEquals(profileScreen.getActionBarTitle(), "Playlist");
    }

    public void testFollowingsClickOpensProfilePage() {
        profileScreen.touchFollowingsTab();
        waiter.waitForContentAndRetryIfLoadingFailed();

        assertTrue(profileScreen.clickUserAt(0).isVisible());
    }

    public void testFollowersClickOpensProfilePage() {
        profileScreen.touchFollowersTab();
        waiter.waitForContentAndRetryIfLoadingFailed();

        assertTrue(profileScreen.clickUserAt(0).isVisible());
    }

    public void testHasAlbumsBucket() {
        assertTrue(profileScreen.getAlbumsTitle().hasVisibility());
    }

    public void testViewAllAlbums() {
        profileScreen.clickViewAllAlbums();
        assertEquals(profileScreen.getActionBarTitle(),
                    ressourceString(R.string.user_profile_sounds_header_albums));
    }

    public void testHasRepostBucket() {
        assertTrue(profileScreen.getRepostsTitle().hasVisibility());
    }

    public void testViewAllReposts() {
        profileScreen.clickViewAllReposts();
        assertEquals(profileScreen.getActionBarTitle(),
                    ressourceString(R.string.user_profile_sounds_header_reposts));
    }

    public void testHasLikesBucket() {
        assertTrue(profileScreen.getLikesTitle().hasVisibility());
    }

    public void testViewAllLikes() {
        profileScreen.clickViewAllLikes();
        assertEquals(profileScreen.getActionBarTitle(),
                    ressourceString(R.string.user_profile_sounds_header_likes));
    }
}
