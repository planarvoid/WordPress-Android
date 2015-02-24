package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.model.UnknownResource;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.TestHttpResponses;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.api.Request;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class ApiClientTest {

    private static final String PUBLIC_API_HOST = "http://api.soundcloud.com";
    private static final String MOBILE_API_HOST = "http://api-mobile.soundcloud.com";
    private static final String PATH = "/path/to/resource";
    private static final String JSON_DATA = "{}";
    private static final String CLIENT_ID = "testClientId";

    private ApiClient apiClient;

    @Mock private OkHttpClient httpClient;
    @Mock private FeatureFlags featureFlags;
    @Mock private JsonTransformer jsonTransformer;
    @Mock private HttpProperties httpProperties;
    @Mock private DeviceHelper deviceHelper;
    @Mock private UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    @Mock private OAuth oAuth;
    @Mock private Call httpCall;

    @Captor private ArgumentCaptor<Request> apiRequestCaptor;
    @Captor private ArgumentCaptor<com.squareup.okhttp.Request> httpRequestCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(deviceHelper.getUserAgent()).thenReturn("");
        when(oAuth.getClientId()).thenReturn(CLIENT_ID);
        when(oAuth.getAuthorizationHeaderValue()).thenReturn("OAuth 12345");
        when(httpClient.newCall(httpRequestCaptor.capture())).thenReturn(httpCall);
        apiClient = new ApiClient(httpClient, new ApiUrlBuilder(httpProperties), jsonTransformer,
                deviceHelper, oAuth, unauthorisedRequestRegistry);
    }

    @Test
    public void shouldMakeSuccessfulGetRequestToPublicApi() throws Exception {
        fakePublicApiResponse();

        ApiRequest request = ApiRequest.Builder.get(PATH).forPublicApi().build();
        ApiResponse response = apiClient.fetchResponse(request);

        expect(response.isSuccess()).toBeTrue();
        expect(response.hasResponseBody()).toBeTrue();
        expect(httpRequestCaptor.getValue().method()).toEqual("GET");
        expect(httpRequestCaptor.getValue().urlString()).toStartWith(PUBLIC_API_HOST + PATH);
    }

    @Test
    public void shouldMakeSuccessfulGetRequestToMobileApi() throws Exception {
        fakeApiMobileResponse();

        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).build();
        ApiResponse response = apiClient.fetchResponse(request);

        expect(response.isSuccess()).toBeTrue();
        expect(response.hasResponseBody()).toBeTrue();
        expect(httpRequestCaptor.getValue().method()).toEqual("GET");
        expect(httpRequestCaptor.getValue().urlString()).toStartWith(MOBILE_API_HOST + PATH);
    }

    @Test
    public void shouldMakeRequestToMobileApiWithAbsoluteUrl() throws Exception {
        fakeApiMobileResponse();

        ApiRequest request = ApiRequest.Builder.get(MOBILE_API_HOST + PATH).forPrivateApi(1).build();
        ApiResponse response = apiClient.fetchResponse(request);

        expect(response.isSuccess()).toBeTrue();
        expect(httpRequestCaptor.getValue().urlString()).toStartWith(MOBILE_API_HOST + PATH);
    }

    @Test
    public void shouldSendUserAgentHeader() throws Exception {
        when(deviceHelper.getUserAgent()).thenReturn("agent");
        fakeApiMobileResponse();

        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).build();
        ApiResponse response = apiClient.fetchResponse(request);

        expect(response.isSuccess()).toBeTrue();
        expect(httpRequestCaptor.getValue().header("User-Agent")).toEqual("agent");
    }

    @Test
    public void shouldMakeRequestWithQueryParameter() throws Exception {
        fakeApiMobileResponse();
        ApiRequest request = ApiRequest.Builder.get(PATH)
                .forPrivateApi(1)
                .addQueryParam("k1", "v1")
                .addQueryParam("k2", "v2")
                .build();

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().urlString()).toEqual(MOBILE_API_HOST + "/path/to/resource?k1=v1&k2=v2&client_id=" + CLIENT_ID);
    }

    @Test
    public void shouldForwardRequestHeaders() throws Exception {
        fakeApiMobileResponse();
        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).withHeader("key", "value").build();

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().headers("key")).toContainExactly("value");
    }

    @Test
    public void shouldSynthesizeContentTypeHeaderWithVersionForMobileApiRequests() throws Exception {
        fakeApiMobileResponse();
        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).build();

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().headers("Accept")).toContainExactly("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldSynthesizeAcceptHeaderForPublicApiRequests() throws Exception {
        fakePublicApiResponse();
        ApiRequest request = ApiRequest.Builder.get(PATH).forPublicApi().build();

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().headers("Accept")).toContainExactly("application/json");
    }

    @Test
    public void shouldAddOAuthHeader() throws Exception {
        fakeApiMobileResponse();
        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).build();

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().headers("Authorization")).toContainExactly("OAuth 12345");
    }

    @Test
    public void shouldAddOAuthClientIdParameterIfMissing() throws Exception {
        fakeApiMobileResponse();
        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).build();

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().urlString()).toEqual(MOBILE_API_HOST + "/path/to/resource?client_id=" + CLIENT_ID);
    }

    @Test
    public void shouldNotAddOAuthClientIdIfAlreadyGiven() throws Exception {
        fakeApiMobileResponse();
        ApiRequest request = ApiRequest.Builder.get(PATH)
                .forPrivateApi(1)
                .addQueryParam("client_id", "123")
                .build();

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().urlString()).toEqual(MOBILE_API_HOST + "/path/to/resource?client_id=123");
    }

    @Test
    public void shouldRegister401sWithUnauthorizedRequestRegistry() throws IOException {
        when(httpProperties.getMobileApiBaseUrl()).thenReturn(MOBILE_API_HOST);
        when(httpCall.execute()).thenReturn(
                TestHttpResponses.response(200).build(),
                TestHttpResponses.response(401).build());
        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).build();
        apiClient.fetchResponse(request); // 200 -- no interaction with registry expected
        verifyZeroInteractions(unauthorisedRequestRegistry);
        apiClient.fetchResponse(request); // 401 -- interaction with registry expected
        verify(unauthorisedRequestRegistry).updateObservedUnauthorisedRequestTimestamp();
    }

    @Test
    public void shouldMakePostRequestToApiMobileWithJsonContentProvidedInRequest() throws Exception {
        fakeApiMobileResponse();
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.Builder.post(PATH)
                .forPrivateApi(1)
                .withContent(new ApiTrack())
                .build();

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().method()).toEqual("POST");
        expect(httpRequestCaptor.getValue().body().contentLength()).toEqual((long) JSON_DATA.length());
        expect(httpRequestCaptor.getValue().body().contentType().toString()).toEqual("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldMakePostRequestToApiMobileWithoutContent() throws Exception {
        fakeApiMobileResponse();
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.Builder.post(PATH)
                .forPrivateApi(1)
                .build();

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().method()).toEqual("POST");
        expect(httpRequestCaptor.getValue().body().contentLength()).toEqual(0L);
        expect(httpRequestCaptor.getValue().body().contentType().toString()).toEqual("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldMakePostRequestsToPublicApiWithoutCharsetsInContentType() throws Exception {
        fakePublicApiResponse();
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.Builder.post(PATH)
                .forPublicApi()
                .withContent(new ApiTrack())
                .build();

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().method()).toEqual("POST");
        expect(httpRequestCaptor.getValue().body().contentLength()).toEqual((long) JSON_DATA.length());
        expect(httpRequestCaptor.getValue().body().contentType().toString()).toEqual("application/json");
    }

    @Test
    public void shouldMakePutRequestToApiMobileWithJsonContentProvidedInRequest() throws Exception {
        fakeApiMobileResponse();
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.Builder.put(PATH)
                .forPrivateApi(1)
                .withContent(new ApiTrack())
                .build();

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().method()).toEqual("PUT");
        expect(httpRequestCaptor.getValue().body().contentLength()).toEqual((long) JSON_DATA.length());
        expect(httpRequestCaptor.getValue().body().contentType().toString()).toEqual("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldFailPostRequestAsMalformedIfContentSerializationFails() throws Exception {
        when(jsonTransformer.toJson(new ApiTrack())).thenThrow(new ApiMapperException("fail"));
        ApiRequest request = ApiRequest.Builder.post(PATH)
                .forPublicApi()
                .withContent(new ApiTrack())
                .build();
        ApiResponse response = apiClient.fetchResponse(request);
        expect(response.isSuccess()).toBeFalse();
        expect(response.getFailure().reason()).toBe(ApiRequestException.Reason.MALFORMED_INPUT);
    }

    @Test
    public void shouldSendDeleteRequest() throws Exception {
        fakeApiMobileResponse();
        ApiRequest request = ApiRequest.Builder.delete(PATH)
                .forPrivateApi(1)
                .build();

        ApiResponse response = apiClient.fetchResponse(request);

        expect(response.isSuccess()).toBeTrue();
        expect(httpRequestCaptor.getValue().method()).toEqual("DELETE");
    }

    @Test
    public void shouldFetchResourcesMappedToTypeSpecifiedInRequest() throws Exception {
        fakeApiMobileResponse(200, JSON_DATA);
        final ApiTrack mappedTrack = new ApiTrack();
        when(jsonTransformer.fromJson(JSON_DATA, TypeToken.of(ApiTrack.class))).thenReturn(mappedTrack);
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(PATH)
                .forPrivateApi(1)
                .forResource(TypeToken.of(ApiTrack.class))
                .build();
        ApiTrack resource = apiClient.fetchMappedResponse(request);
        expect(resource).toBe(mappedTrack);
    }

    @Test(expected = ApiMapperException.class)
    public void shouldThrowMappingExceptionIfParsedToUnknownResource() throws Exception {
        fakeApiMobileResponse(200, JSON_DATA);
        when(jsonTransformer.fromJson(JSON_DATA, TypeToken.of(ApiTrack.class))).thenReturn(new UnknownResource());
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(PATH)
                .forPrivateApi(1)
                .forResource(TypeToken.of(ApiTrack.class))
                .build();
        apiClient.fetchMappedResponse(request);
    }

    @Test(expected = ApiMapperException.class)
    public void shouldThrowMappingExceptionIfResponseBodyIsBlank() throws Exception {
        fakeApiMobileResponse(200, "");
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(PATH)
                .forPrivateApi(1)
                .forResource(TypeToken.of(ApiTrack.class))
                .build();
        apiClient.fetchMappedResponse(request);
    }

    @Test(expected = ApiRequestException.class)
    public void shouldThrowMappingExceptionIfResponseWasUnsuccessful() throws Exception {
        fakeApiMobileResponse(500, "");
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(PATH)
                .forPrivateApi(1)
                .forResource(TypeToken.of(ApiTrack.class))
                .build();
        apiClient.fetchMappedResponse(request);
    }

    private void fakeApiMobileResponse() throws IOException {
        when(httpProperties.getMobileApiBaseUrl()).thenReturn(MOBILE_API_HOST);
        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());
    }

    private void fakeApiMobileResponse(int code, String jsonBody) throws IOException {
        when(httpProperties.getMobileApiBaseUrl()).thenReturn(MOBILE_API_HOST);
        when(httpCall.execute()).thenReturn(TestHttpResponses.jsonResponse(code, jsonBody).build());
    }

    private void fakePublicApiResponse() throws IOException {
        when(httpProperties.getPublicApiBaseUrl()).thenReturn(PUBLIC_API_HOST);
        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());
    }

}
