package com.soundcloud.android.analytics.playcounts;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.urlEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlayCountUrlBuilderTest extends AndroidUnitTest {

    private static final int VERSION_CODE = 12;
    private PlayCountUrlBuilder urlBuilder;

    @Mock OAuth oauth;
    @Mock AccountOperations accountOperations;
    @Mock DeviceHelper deviceHelper;

    @Before
    public void setup() {
        when(oauth.getClientId()).thenReturn("ABCDEF");
        when(accountOperations.getSoundCloudToken()).thenReturn(Token.EMPTY);
        when(deviceHelper.getAppVersionCode()).thenReturn(VERSION_CODE);
        urlBuilder = new PlayCountUrlBuilder(resources(), deviceHelper, oauth, accountOperations);
    }

    @Test
    public void shouldBuildPlayCountTrackingUrlFromPlaySessionEvent() throws CreateModelException {
        PlaybackSessionEvent event = TestEvents.playbackSessionPlayEvent();

        final String url = urlBuilder.buildUrl(event);

        assertThat(url, urlEqualTo("https://api.soundcloud.com/tracks/1/plays?client_id=ABCDEF&policy=ALLOW&client_event_id=uuid&ts=1000&client_id=3152&app_version=12"));
    }

    @Test
    public void shouldAppendOauthTokenIfUserIsLoggedIn() throws CreateModelException {
        when(accountOperations.getSoundCloudToken()).thenReturn(new Token("access", "refresh"));
        PlaybackSessionEvent event = TestEvents.playbackSessionPlayEvent();

        final String url = urlBuilder.buildUrl(event);

        assertThat(url, urlEqualTo("https://api.soundcloud.com/tracks/1/plays?client_id=ABCDEF&oauth_token=access&policy=ALLOW&client_event_id=uuid&ts=1000&client_id=3152&app_version=12"));
    }

    @Test
    public void shouldNotAppendPolicyIfNull() {
        final PropertySet policyMissing = PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                TrackProperty.CREATOR_URN.bind(Urn.forTrack(456L)),
                TrackProperty.MONETIZATION_MODEL.bind(TestPropertySets.MONETIZATION_MODEL),
                PlayableProperty.PLAY_DURATION.bind(1000L)
        );
        PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(policyMissing, Urn.forUser(1), null, 0, 1000L, "hls", "playa", "3g", false, false, "uuid");

        final String url = urlBuilder.buildUrl(event);

        assertThat(url, urlEqualTo("https://api.soundcloud.com/tracks/123/plays?client_id=ABCDEF&client_event_id=uuid&ts=1000&client_id=3152&app_version=12"));
    }
}
