package com.soundcloud.android.navigation;

import static com.soundcloud.android.model.Urn.forAd;
import static com.soundcloud.android.model.Urn.forUser;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.ads.FullScreenVideoActivity;
import com.soundcloud.android.ads.PrestitialActivity;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.profile.UserAlbumsActivity;
import com.soundcloud.android.profile.UserLikesActivity;
import com.soundcloud.android.profile.UserPlaylistsActivity;
import com.soundcloud.android.profile.UserRepostsActivity;
import com.soundcloud.android.profile.UserTracksActivity;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.assertions.IntentAssert;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class IntentFactoryTest extends AndroidUnitTest {

    @Mock Context context;

    @Test
    public void openAdClickthrough() {
        final Uri uri = Uri.parse("http://clickthroughurl.com");
        assertIntent(IntentFactory.createAdClickthroughIntent(uri))
                .containsAction(Intent.ACTION_VIEW)
                .containsUri(uri)
                .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void openVideoFullScreen() {
        final Urn urn = forAd("network", "123");
        assertIntent(IntentFactory.createFullscreenVideoAdIntent(context, urn))
                .containsExtra(FullScreenVideoActivity.EXTRA_AD_URN, urn)
                .opensActivity(FullScreenVideoActivity.class);
    }

    @Test
    public void openVisualPrestitial() {
        assertIntent(IntentFactory.createPrestititalAdIntent(context))
                .opensActivity(PrestitialActivity.class);
    }

    @Test
    public void openProfile() {
        final Urn urn = forUser(123);
        final Screen screen = Screen.UNKNOWN;
        final SearchQuerySourceInfo searchQuerySourceInfo = mock(SearchQuerySourceInfo.class);
        final Referrer referrer = Referrer.PLAYBACK_WIDGET;
        assertIntent(IntentFactory.createProfileIntent(context, urn, Optional.of(screen), Optional.of(searchQuerySourceInfo), Optional.of(referrer)))
                .containsExtra(ProfileActivity.EXTRA_USER_URN, urn)
                .containsExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo)
                .containsScreen(screen)
                .containsReferrer(referrer)
                .opensActivity(ProfileActivity.class);
    }

    @Test
    public void openProfileReposts() {
        final Urn urn = forUser(123);
        final Screen screen = Screen.UNKNOWN;
        final SearchQuerySourceInfo searchQuerySourceInfo = mock(SearchQuerySourceInfo.class);
        assertIntent(IntentFactory.createProfileRepostsIntent(context, urn, screen, Optional.of(searchQuerySourceInfo)))
                .containsExtra(UserRepostsActivity.EXTRA_USER_URN, urn)
                .containsExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo)
                .containsScreen(screen)
                .opensActivity(UserRepostsActivity.class);
    }

    @Test
    public void openProfileTracks() {
        final Urn urn = forUser(123);
        final Screen screen = Screen.UNKNOWN;
        final SearchQuerySourceInfo searchQuerySourceInfo = mock(SearchQuerySourceInfo.class);
        assertIntent(IntentFactory.createProfileTracksIntent(context, urn, screen, Optional.of(searchQuerySourceInfo)))
                .containsExtra(UserTracksActivity.EXTRA_USER_URN, urn)
                .containsExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo)
                .containsScreen(screen)
                .opensActivity(UserTracksActivity.class);
    }

    @Test
    public void openProfileLikes() {
        final Urn urn = forUser(123);
        final Screen screen = Screen.UNKNOWN;
        final SearchQuerySourceInfo searchQuerySourceInfo = mock(SearchQuerySourceInfo.class);
        assertIntent(IntentFactory.createProfileLikesIntent(context, urn, screen, Optional.of(searchQuerySourceInfo)))
                .containsExtra(UserLikesActivity.EXTRA_USER_URN, urn)
                .containsExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo)
                .containsScreen(screen)
                .opensActivity(UserLikesActivity.class);
    }

    @Test
    public void openProfileAlbums() {
        final Urn urn = forUser(123);
        final Screen screen = Screen.UNKNOWN;
        final SearchQuerySourceInfo searchQuerySourceInfo = mock(SearchQuerySourceInfo.class);
        assertIntent(IntentFactory.createProfileAlbumsIntent(context, urn, screen, Optional.of(searchQuerySourceInfo)))
                .containsExtra(UserAlbumsActivity.EXTRA_USER_URN, urn)
                .containsExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo)
                .containsScreen(screen)
                .opensActivity(UserAlbumsActivity.class);
    }

    @Test
    public void openProfilePlaylists() {
        final Urn urn = forUser(123);
        final Screen screen = Screen.UNKNOWN;
        final SearchQuerySourceInfo searchQuerySourceInfo = mock(SearchQuerySourceInfo.class);
        assertIntent(IntentFactory.createProfilePlaylistsIntent(context, urn, screen, Optional.of(searchQuerySourceInfo)))
                .containsExtra(UserPlaylistsActivity.EXTRA_USER_URN, urn)
                .containsExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo)
                .containsScreen(screen)
                .opensActivity(UserPlaylistsActivity.class);
    }

    private IntentAssert assertIntent(Intent intent) {
        return new IntentAssert(intent);
    }
}
