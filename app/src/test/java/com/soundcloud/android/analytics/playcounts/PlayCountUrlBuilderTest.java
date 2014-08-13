package com.soundcloud.android.analytics.playcounts;

import static com.soundcloud.android.matchers.SoundCloudMatchers.urlEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.TestPropertySets;
import com.soundcloud.android.api.HttpProperties;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.api.Token;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlayCountUrlBuilderTest {

    private static final PropertySet TRACK_DATA = TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(123L));

    private PlayCountUrlBuilder urlBuilder;

    @Mock HttpProperties httpProperties;
    @Mock AccountOperations accountOperations;

    @Before
    public void setup() {
        when(httpProperties.getClientId()).thenReturn("ABCDEF");
        urlBuilder = new PlayCountUrlBuilder(httpProperties, accountOperations);
    }

    @Test
    public void shouldBuildPlayCountTrackingUrlFromPlaySessionEvent() {
        PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(TRACK_DATA, Urn.forUser(1), null, 0, 1000L);

        final String url = urlBuilder.buildUrl(event);

        assertThat(url, urlEqualTo("https://api.soundcloud.com/tracks/123/plays?client_id=ABCDEF&policy=allow"));
    }

    @Test
    public void shouldAppendOauthTokenIfUserIsLoggedIn() {
        when(accountOperations.getSoundCloudToken()).thenReturn(new Token("access", "refresh"));
        PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(TRACK_DATA, Urn.forUser(1), null, 0, 1000L);

        final String url = urlBuilder.buildUrl(event);

        assertThat(url, urlEqualTo("https://api.soundcloud.com/tracks/123/plays?client_id=ABCDEF&oauth_token=access&policy=allow"));
    }

    @Test
    public void shouldNotAppendPolicyIfNull() {
        final PropertySet policyMissing = PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                PlayableProperty.DURATION.bind(1000)
        );
        PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(policyMissing, Urn.forUser(1), null, 0, 1000L);

        final String url = urlBuilder.buildUrl(event);

        assertThat(url, urlEqualTo("https://api.soundcloud.com/tracks/123/plays?client_id=ABCDEF"));
    }
}