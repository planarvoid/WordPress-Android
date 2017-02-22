package com.soundcloud.android.api;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.ListMultiMap;
import com.soundcloud.java.collections.MultiMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.res.Resources;

public class ApiUrlBuilderTest extends AndroidUnitTest {

    private ApiUrlBuilder urlBuilder;

    @Mock private Resources resources;
    @Mock private OAuth oAuth;

    private String publicApiBaseUrl;
    private String mobileApiBaseUrl;

    @Before
    public void setup() {
        publicApiBaseUrl = context().getResources().getString(R.string.public_api_base_url);
        mobileApiBaseUrl = context().getResources().getString(R.string.mobile_api_base_url);

        when(oAuth.getClientId()).thenReturn("test_client_id");

        urlBuilder = new ApiUrlBuilder(publicApiBaseUrl, mobileApiBaseUrl, oAuth);
    }

    @Test
    public void shouldBuildFullUrlFromApiMobileEndpoint() {
        final String url = urlBuilder.from(ApiEndpoints.SEARCH_TRACKS).build();
        final String expectedUrl = mobileApiBaseUrl + "/search/tracks?client_id=test_client_id";
        assertThat(url).isEqualTo(expectedUrl);
    }

    @Test
    public void shouldBuildFullUrlFromApiMobileEndpointWithPathParameters() {
        final String url = urlBuilder.from(ApiEndpoints.ADS, "soundcloud:tracks:1").build();
        final String expectedUrl = mobileApiBaseUrl + "/tracks/soundcloud:tracks:1/ads?client_id=test_client_id";
        assertThat(url).isEqualTo(expectedUrl);
    }

    @Test
    public void shouldBuildFullUrlFromApiMobileRequestWithRelativeUrl() {
        final ApiRequest request = ApiRequest.get("/path").forPrivateApi().build();
        final String url = urlBuilder.from(request).build();
        final String expectedUrl = mobileApiBaseUrl + "/path?client_id=test_client_id";
        assertThat(url).isEqualTo(expectedUrl);
    }

    @Test
    public void shouldBuildFullUrlFromApiMobileRequestWithAbsoluteUrl() {
        final ApiRequest request = ApiRequest.get("http://api.com/path").forPrivateApi().build();
        final String url = urlBuilder.from(request).build();
        assertThat(url).isEqualTo("http://api.com/path?client_id=test_client_id");
    }

    @Test
    public void shouldBuildUrlFromPublicApiRequest() {
        final ApiRequest request = ApiRequest.get("/path").forPublicApi().build();
        final String url = urlBuilder.from(request).build();
        final String expectedUrl = publicApiBaseUrl + "/path?client_id=test_client_id";
        assertThat(url).isEqualTo(expectedUrl);
    }

    @Test
    public void shouldAddAllQueryParamsToUrlAsCommaSeparatedValuesForMultipleValues() {
        final MultiMap<String, String> params = new ListMultiMap<>();
        params.putAll("k", asList("v1", "v2"));
        final String url = urlBuilder.from(ApiEndpoints.SEARCH_TRACKS)
                                     .withQueryParams(params)
                                     .build();
        final String expectedUrl = mobileApiBaseUrl + "/search/tracks?client_id=test_client_id&k=v1%2Cv2";
        assertThat(url).isEqualTo(expectedUrl);
    }

    @Test
    public void shouldAddSingleStringQueryParamToUrl() {
        final String url = urlBuilder.from(ApiEndpoints.SEARCH_TRACKS).withQueryParam("a", 1).build();
        final String expectedUrl = mobileApiBaseUrl + "/search/tracks?client_id=test_client_id&a=1";
        assertThat(url).isEqualTo(expectedUrl);
    }

    @Test
    public void shouldAddSingleConstantQueryParamToUrl() {
        final String url = urlBuilder.from(ApiEndpoints.SEARCH_TRACKS)
                                     .withQueryParam(ApiRequest.Param.OAUTH_TOKEN, "x")
                                     .build();
        final String expectedUrl = mobileApiBaseUrl + "/search/tracks?client_id=test_client_id&oauth_token=x";
        assertThat(url).isEqualTo(expectedUrl);
    }
}
