package com.soundcloud.android.playback;

import static com.soundcloud.android.matchers.SoundCloudMatchers.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;

@RunWith(SoundCloudTestRunner.class)
public class StreamUrlBuilderTest {

    @Mock AccountOperations accountOperations;
    @Mock Token token;
    @Mock Resources resources;
    @Mock OAuth oAuth;

    private StreamUrlBuilder streamUrlBuilder;
    private Urn trackUrn;

    @Before
    public void setUp() throws Exception {
        when(accountOperations.getSoundCloudToken()).thenReturn(token);
        when(resources.getString(R.string.mobile_api_base_url)).thenReturn("https://api-mobile");
        when(oAuth.getClientId()).thenReturn("clientId");
        when(token.getAccessToken()).thenReturn("token");
        when(token.valid()).thenReturn(true);

        ApiUrlBuilder apiUrlBuilder = new ApiUrlBuilder(resources, oAuth);

        streamUrlBuilder = new StreamUrlBuilder(accountOperations, apiUrlBuilder);
        trackUrn = Urn.forTrack(2L);
    }

    @Test
    public void buildsHttpsStreamUrl() throws Exception {
        String result = streamUrlBuilder.buildHttpsStreamUrl(trackUrn);

        assertThat(result, urlEqualTo("https://api-mobile/tracks/soundcloud:tracks:2/streams/https?client_id=clientId&oauth_token=token"));
    }

    @Test
    public void buildsHttpStreamUrl() throws Exception {
        String result = streamUrlBuilder.buildHttpStreamUrl(trackUrn);

        assertThat(result, urlEqualTo("https://api-mobile/tracks/soundcloud:tracks:2/streams/http?client_id=clientId&oauth_token=token"));
    }

}
