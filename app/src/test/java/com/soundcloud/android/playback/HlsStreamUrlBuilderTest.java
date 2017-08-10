package com.soundcloud.android.playback;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAdSource;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class HlsStreamUrlBuilderTest extends AndroidUnitTest {

    private static final String TOKEN = "token";
    private static final Long URN_ID = 123L;
    private static final Urn URN = Urn.forTrack(URN_ID);
    private static final String CLIENT_ID = "clientId";
    @Mock private AccountOperations accountOperations;
    @Mock private SecureFileStorage secureFileStorage;
    @Mock private Token token;
    @Mock private OAuth oAuth;

    private HlsStreamUrlBuilder hlsStreamUrlBuilder;

    @Before
    public void setUp() throws Exception {
        when(accountOperations.getSoundCloudToken()).thenReturn(token);
        when(accountOperations.hasValidToken()).thenReturn(true);
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(token.getAccessToken()).thenReturn(TOKEN);
        when(oAuth.getClientId()).thenReturn(CLIENT_ID);
        when(token.valid()).thenReturn(true);

        final ApiUrlBuilder apiUrlBuilder = new ApiUrlBuilder(context().getResources().getString(R.string.public_api_base_url), mobileApiBaseUrl(), oAuth);
        hlsStreamUrlBuilder = new HlsStreamUrlBuilder(accountOperations, secureFileStorage, apiUrlBuilder);
    }

    @Test
    public void buildsCorrectUrlForPreloadingTrack() {
        final String expected = mobileApiBaseUrl() + "/tracks/soundcloud:tracks:" + URN_ID + "/streams/hls?client_id=" +
                CLIENT_ID + "&oauth_token=" + TOKEN + "&can_snip=false";

        String streamUrl = hlsStreamUrlBuilder.buildStreamUrl(new AutoValue_PreloadItem(URN.getContent(), PlaybackType.AUDIO_DEFAULT));
        assertThat(streamUrl).isEqualTo(expected);
    }

    @Test
    public void buildsCorrectUrlForPreloadingAudioSnippet() {
        final String expected = mobileApiBaseUrl() + "/tracks/soundcloud:tracks:" + URN_ID + "/streams/hls/snippet?client_id=" +
                CLIENT_ID + "&oauth_token=" + TOKEN;

        String streamUrl = hlsStreamUrlBuilder.buildStreamUrl(new AutoValue_PreloadItem(URN.getContent(), PlaybackType.AUDIO_SNIPPET));
        assertThat(streamUrl).isEqualTo(expected);
    }

    @Test
    public void buildsCorrectUrlForStreamingTrack() {
        final String expected = mobileApiBaseUrl() + "/tracks/soundcloud:tracks:" + URN_ID + "/streams/hls?client_id=" +
                CLIENT_ID + "&oauth_token=" + TOKEN + "&can_snip=false";

        String streamUrl = hlsStreamUrlBuilder.buildStreamUrl(AudioPlaybackItem.create(URN, 0L, 0L, PlaybackType.AUDIO_DEFAULT));
        assertThat(streamUrl).isEqualTo(expected);
    }

    @Test
    public void buildsCorrectUrlForStreamingSnippet() {
        final String expected = mobileApiBaseUrl() + "/tracks/soundcloud:tracks:" + URN_ID + "/streams/hls/snippet?client_id=" +
                CLIENT_ID + "&oauth_token=" + TOKEN;

        String streamUrl = hlsStreamUrlBuilder.buildStreamUrl(AudioPlaybackItem.create(URN, 0L, 0L, PlaybackType.AUDIO_SNIPPET));
        assertThat(streamUrl).isEqualTo(expected);
    }

    @Test
    public void buildsCorrectUrlForStreamingAudioAd() {
        AudioAdPlaybackItem audioAdPlaybackItem = AudioAdPlaybackItem.create(AdFixtures.getAudioAd(URN));
        final String expected = getHlsAudioSource(audioAdPlaybackItem).url() + "?oauth_token=" + TOKEN;

        String streamUrl = hlsStreamUrlBuilder.buildStreamUrl(audioAdPlaybackItem);
        assertThat(streamUrl).isEqualTo(expected);
    }

    private AudioAdSource getHlsAudioSource(AudioAdPlaybackItem audioAdPlaybackItem) {
        return Iterables.find(audioAdPlaybackItem.getSources(), AudioAdSource::isHls);
    }

    @Test
    public void buildsCorrectUrlForStreamingVideoAd() {
        final PlaybackItem videoAdPlaybackItem = VideoAdPlaybackItem.create(AdFixtures.getVideoAd(URN), 0L);
        final String expected = mobileApiBaseUrl() + "/tracks/" + videoAdPlaybackItem.getUrn() +
                "/streams/hls?client_id=" + CLIENT_ID + "&oauth_token=" + TOKEN + "&can_snip=false";

        String streamUrl = hlsStreamUrlBuilder.buildStreamUrl(videoAdPlaybackItem);
        assertThat(streamUrl).isEqualTo(expected);
    }

    private String mobileApiBaseUrl() {
        return context().getResources().getString(R.string.mobile_api_base_url);
    }

}
