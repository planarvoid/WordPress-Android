package com.soundcloud.android.api;

import android.content.res.Resources;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Collections;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

@RunWith(SoundCloudTestRunner.class)
public class ApiUrlBuilderTest {

    private ApiUrlBuilder urlBuilder;

    @Mock private Resources resources;

    @Before
    public void setup() {
        when(resources.getString(R.string.mobile_api_base_url)).thenReturn("https://api-mobile.soundcloud.com");
        when(resources.getString(R.string.public_api_base_url)).thenReturn("https://api.soundcloud.com");
        urlBuilder = new ApiUrlBuilder(resources);
    }

    @Test
    public void shouldBuildFullUrlFromApiMobileEndpoint() {
        final String url = urlBuilder.from(ApiEndpoints.SEARCH_TRACKS).build();
        expect(url).toEqual("https://api-mobile.soundcloud.com/search/tracks");
    }

    @Test
    public void shouldBuildFullUrlFromApiMobileEndpointWithPathParameters() {
        final String url = urlBuilder.from(ApiEndpoints.ADS, "soundcloud:tracks:1").build();
        expect(url).toEqual("https://api-mobile.soundcloud.com/tracks/soundcloud:tracks:1/ads");
    }

    @Test
    public void shouldBuildFullUrlFromApiMobileRequestWithRelativeUrl() {
        ApiRequest request = ApiRequest.Builder.get("/path").forPrivateApi(1).build();
        final String url = urlBuilder.from(request).build();
        expect(url).toEqual("https://api-mobile.soundcloud.com/path");
    }

    @Test
    public void shouldBuildFullUrlFromApiMobileRequestWithAbsoluteUrl() {
        ApiRequest request = ApiRequest.Builder.get("http://api.com/path").forPrivateApi(1).build();
        final String url = urlBuilder.from(request).build();
        expect(url).toEqual("http://api.com/path");
    }

    @Test
    public void shouldBuildUrlFromPublicApiRequest() {
        ApiRequest request = ApiRequest.Builder.get("/path").forPublicApi().build();
        final String url = urlBuilder.from(request).build();
        expect(url).toEqual("https://api.soundcloud.com/path");
    }

    @Test
    public void shouldAddAllQueryParamsToUrl() {
        final String url = urlBuilder.from(ApiEndpoints.SEARCH_TRACKS).withQueryParams(Collections.singletonMap("k", "v")).build();
        expect(url).toEqual("https://api-mobile.soundcloud.com/search/tracks?k=v");
    }

    @Test
    public void shouldAddSingleStringQueryParamToUrl() {
        final String url = urlBuilder.from(ApiEndpoints.SEARCH_TRACKS).withQueryParam("a", 1).build();
        expect(url).toEqual("https://api-mobile.soundcloud.com/search/tracks?a=1");
    }

    @Test
    public void shouldAddSingleConstantQueryParamToUrl() {
        final String url = urlBuilder.from(ApiEndpoints.SEARCH_TRACKS).withQueryParam(ApiRequest.Param.OAUTH_TOKEN, "x").build();
        expect(url).toEqual("https://api-mobile.soundcloud.com/search/tracks?oauth_token=x");
    }
}