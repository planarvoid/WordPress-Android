package com.soundcloud.android.tests.discovery;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.soundcloud.android.discovery.systemplaylist.SystemPlaylistActivity.EXTRA_PLAYLIST_URN;
import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.framework.helpers.AssetHelper.readBodyOfFile;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.model.Urn.forSystemPlaylist;
import static com.soundcloud.android.utils.Urns.writeToIntent;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.discovery.systemplaylist.SystemPlaylistActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.screens.discovery.SystemPlaylistScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.utils.Urns;
import org.junit.Test;

import android.content.Intent;
import android.content.res.Resources;

public class SystemPlaylistTest extends ActivityTest<SystemPlaylistActivity> {

    private static final Urn SYSTEM_PLAYLIST_URN = forSystemPlaylist("123");
    private static final Intent START_PARAM_INTENT = writeToIntent(new Intent(), EXTRA_PLAYLIST_URN, SYSTEM_PLAYLIST_URN);

    public SystemPlaylistTest() {
        super(SystemPlaylistActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        setActivityIntent(START_PARAM_INTENT);
    }

    @Override
    protected void addInitialStubMappings() {
        Resources resources = getInstrumentation().getContext().getResources();
        stubFor(get(urlPathMatching("/system-playlists/soundcloud%3Asystem-playlists%3A123"))
                        .willReturn(aResponse().withStatus(200).withBody(readBodyOfFile(resources, "system-playlist.json"))));
    }

    @Test
    public void testSystemPlaylistPlayback() throws Exception {
        mrLocalLocal.startEventTracking();
        final SystemPlaylistScreen systemPlaylistScreen = new SystemPlaylistScreen(solo);

        assertThat(systemPlaylistScreen, is(visible()));

        final String firstTrackTitle = systemPlaylistScreen.getTracks().get(0).getTitle();

        final VisualPlayerElement player = systemPlaylistScreen.clickHeaderPlay();

        assertTrue(player.isExpanded());
        assertThat(firstTrackTitle, equalTo(player.getTrackTitle()));
        mrLocalLocal.verify("specs/system_playlist_play_track.spec");
    }

    @Test
    public void testSystemPlaylistEngagement() throws Exception {
        mrLocalLocal.startEventTracking();
        final SystemPlaylistScreen systemPlaylistScreen = new SystemPlaylistScreen(solo);

        assertThat(systemPlaylistScreen, is(visible()));

        systemPlaylistScreen.toggleTrackLike(0);

        assertTrue(systemPlaylistScreen.getTracks().get(0).clickOverflowButton().isLiked());
        mrLocalLocal.verify("specs/system_playlist_like_track.spec");
    }
}
