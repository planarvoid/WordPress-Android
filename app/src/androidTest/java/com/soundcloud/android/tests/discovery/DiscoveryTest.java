package com.soundcloud.android.tests.discovery;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.framework.helpers.AssetHelper.readBodyOfFile;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.properties.Flag.NEW_HOME;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.SystemPlaylistScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

import android.content.res.Resources;

public class DiscoveryTest extends ActivityTest<MainActivity> {

    private DiscoveryScreen discoveryScreen;

    public DiscoveryTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        getFeatureFlags().enable(NEW_HOME);
    }

    @Override
    protected void addInitialStubMappings() {
        Resources resources = getInstrumentation().getContext().getResources();
        stubFor(get(urlPathMatching("/discovery/cards"))
                        .willReturn(aResponse().withStatus(200).withBody(readBodyOfFile(resources, "discovery-cards.json"))));
        stubFor(get(urlPathMatching("/system-playlists/soundcloud%3Asystem-playlists%3Athe-upload%3Asoundcloud%3Ausers%3A183"))
                        .willReturn(aResponse().withStatus(200).withBody(readBodyOfFile(resources, "system-playlist.json"))));
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.discoveryScreen();
    }

    @Test
    public void testOpensCards() throws Exception {
        mrLocalLocal.startEventTracking();
        SystemPlaylistScreen systemPlaylistScreen = discoveryScreen.singleSelectionCard().clickCard();
        assertThat(systemPlaylistScreen, is(visible()));
        assertThat(systemPlaylistScreen.title().getText(), equalTo("Ambient: New & Hot"));
        discoveryScreen = systemPlaylistScreen.goBack(DiscoveryScreen::new);
        assertTrue(discoveryScreen.isVisible());

        systemPlaylistScreen = discoveryScreen.multipleSelectionCard().clickFirstPlaylist();
        assertThat(systemPlaylistScreen, is(visible()));
        assertThat(systemPlaylistScreen.title().getText(), equalTo("Ambient: New & Hot"));
        discoveryScreen = systemPlaylistScreen.goBack(DiscoveryScreen::new);
        assertTrue(discoveryScreen.isVisible());
        mrLocalLocal.verify("specs/discovery_cards.spec");
    }
}
