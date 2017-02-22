package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.res.Resources;

public class StreamUrlBuilderTest extends AndroidUnitTest {

    @Mock AccountOperations accountOperations;
    @Mock Token token;
    @Mock Resources resources;
    @Mock OAuth oAuth;

    private StreamUrlBuilder streamUrlBuilder;
    private Urn trackUrn;
    private String mobileApiBaseUrl;

    @Before
    public void setUp() throws Exception {
        mobileApiBaseUrl = context().getResources().getString(R.string.mobile_api_base_url);

        when(accountOperations.getSoundCloudToken()).thenReturn(token);
        when(oAuth.getClientId()).thenReturn("clientId");
        when(token.getAccessToken()).thenReturn("token");
        when(token.valid()).thenReturn(true);

        final String publicApiBaseUrl = context().getResources().getString(R.string.public_api_base_url);
        final ApiUrlBuilder apiUrlBuilder = new ApiUrlBuilder(publicApiBaseUrl, mobileApiBaseUrl, oAuth);

        streamUrlBuilder = new StreamUrlBuilder(accountOperations, apiUrlBuilder);
        trackUrn = Urn.forTrack(2L);
    }

    @Test
    public void buildsHttpsStreamUrl() throws Exception {
        final String result = streamUrlBuilder.buildHttpsStreamUrl(trackUrn);
        final String expectedUrl = mobileApiBaseUrl + "/tracks/soundcloud:tracks:2/streams/https?client_id=clientId&oauth_token=token";

        assertThat(result, urlEqualTo(expectedUrl));
    }

    @Test
    public void buildsHttpStreamUrl() throws Exception {
        final String result = streamUrlBuilder.buildHttpStreamUrl(trackUrn);
        final String expectedUrl = mobileApiBaseUrl + "/tracks/soundcloud:tracks:2/streams/http?client_id=clientId&oauth_token=token";

        assertThat(result, urlEqualTo(expectedUrl));
    }
}
