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
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.api.Request;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;

@Ignore
@RunWith(SoundCloudTestRunner.class)
public class ApiClientTest {

    private static final String PATH = "/path/to/resource";
    private static final String JSON_DATA = "{}";
    private static final String CLIENT_ID = "testClientId";

    private ApiClient apiClient;
    private OkHttpClient httpClient = new OkHttpClient();
    private MockWebServer mockWebServer = new MockWebServer();

    @Mock private FeatureFlags featureFlags;
    @Mock private JsonTransformer jsonTransformer;
    @Mock private HttpProperties httpProperties;
    @Mock private DeviceHelper deviceHelper;
    @Mock private UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    @Mock private OAuth oAuth;
    @Captor private ArgumentCaptor<Request> requestCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(deviceHelper.getUserAgent()).thenReturn("");
        when(oAuth.getClientId()).thenReturn(CLIENT_ID);
        when(oAuth.getAuthorizationHeaderValue()).thenReturn("OAuth 12345");
        apiClient = new ApiClient(httpClient, new ApiUrlBuilder(httpProperties), jsonTransformer,
                deviceHelper, oAuth, unauthorisedRequestRegistry);
        mockWebServer.play();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void shouldMakeSuccessfulGetRequestToPublicApi() throws Exception {
        fakePublicApiResponse(new MockResponse().setBody("ok"));

        ApiRequest request = ApiRequest.Builder.get(PATH).forPublicApi().build();
        ApiResponse response = apiClient.fetchResponse(request);

        expect(response.isSuccess()).toBeTrue();
        expect(response.hasResponseBody()).toBeTrue();
        expect(response.getResponseBody()).toEqual("ok");
        final RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getMethod()).toEqual("GET");
        expect(recordedRequest.getPath()).toStartWith(PATH);
    }

    @Test
    public void shouldMakeSuccessfulGetRequestToMobileApi() throws Exception {
        fakeApiMobileResponse(new MockResponse().setBody("ok"));

        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).build();
        ApiResponse response = apiClient.fetchResponse(request);

        expect(response.isSuccess()).toBeTrue();
        expect(response.hasResponseBody()).toBeTrue();
        expect(response.getResponseBody()).toEqual("ok");
        final RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getMethod()).toEqual("GET");
        expect(recordedRequest.getPath()).toStartWith(PATH);
    }

    @Test
    public void shouldMakeRequestToMobileApiWithAbsoluteUrl() throws Exception {
        fakeApiMobileResponse(new MockResponse());

        ApiRequest request = ApiRequest.Builder.get(mockWebServer.getUrl(PATH).toString()).
                forPrivateApi(1).build();
        ApiResponse response = apiClient.fetchResponse(request);

        final RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(response.isSuccess()).toBeTrue();
        expect(recordedRequest.getPath()).toStartWith(PATH);
    }

    @Test
    public void shouldSendUserAgentHeader() throws Exception {
        when(deviceHelper.getUserAgent()).thenReturn("agent");
        fakeApiMobileResponse(new MockResponse());

        ApiRequest request = ApiRequest.Builder.get(mockWebServer.getUrl(PATH).toString()).
                forPrivateApi(1).build();
        ApiResponse response = apiClient.fetchResponse(request);

        final RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(response.isSuccess()).toBeTrue();
        expect(recordedRequest.getHeader("User-Agent")).toEqual("agent");
    }

    @Test
    public void shouldRequestGzippedResponsesByDefault() throws Exception {
        fakeApiMobileResponse(new MockResponse());

        ApiRequest request = ApiRequest.Builder.get(mockWebServer.getUrl(PATH).toString()).
                forPrivateApi(1).build();
        ApiResponse response = apiClient.fetchResponse(request);

        final RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(response.isSuccess()).toBeTrue();
        expect(recordedRequest.getHeader("Accept-Encoding")).toEqual("gzip");
    }

    @Test
    public void shouldMakeRequestWithQueryParameter() throws Exception {
        fakeApiMobileResponse(new MockResponse());
        ApiRequest request = ApiRequest.Builder.get(PATH)
                .forPrivateApi(1)
                .addQueryParam("k1", "v1")
                .addQueryParam("k2", "v2")
                .build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getPath()).toEqual("/path/to/resource?k1=v1&k2=v2&client_id=" + CLIENT_ID);
    }

    @Test
    public void shouldForwardRequestHeaders() throws Exception {
        fakeApiMobileResponse(new MockResponse());
        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).withHeader("key", "value").build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getHeaders("key")).toContainExactly("value");
    }

    @Test
    public void shouldSynthesizeContentTypeHeaderWithVersionForMobileApiRequests() throws Exception {
        fakeApiMobileResponse(new MockResponse());
        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getHeaders("Accept")).toContainExactly("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldSynthesizeAcceptHeaderForPublicApiRequests() throws Exception {
        fakePublicApiResponse(new MockResponse());
        ApiRequest request = ApiRequest.Builder.get(PATH).forPublicApi().build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getHeaders("Accept")).toContainExactly("application/json");
    }

    @Test
    public void shouldAddOAuthHeader() throws Exception {
        fakeApiMobileResponse(new MockResponse());
        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getHeaders("Authorization")).toContainExactly("OAuth 12345");
    }

    @Test
    public void shouldAddOAuthClientIdParameterIfMissing() throws Exception {
        fakeApiMobileResponse(new MockResponse());
        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getPath()).toEqual("/path/to/resource?client_id=" + CLIENT_ID);
    }

    @Test
    public void shouldNotAddOAuthClientIdIfAlreadyGiven() throws Exception {
        fakeApiMobileResponse(new MockResponse());
        ApiRequest request = ApiRequest.Builder.get(PATH)
                .forPrivateApi(1)
                .addQueryParam("client_id", "123")
                .build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getPath()).toEqual("/path/to/resource?client_id=123");
    }

    @Test
    public void shouldRegister401sWithUnauthorizedRequestRegistry() throws IOException {
        fakeApiMobileResponse(new MockResponse().setResponseCode(200), new MockResponse().setResponseCode(401));
        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).build();
        apiClient.fetchResponse(request); // 200 -- no interaction with registry expected
        verifyZeroInteractions(unauthorisedRequestRegistry);
        apiClient.fetchResponse(request); // 401 -- interaction with registry expected
        verify(unauthorisedRequestRegistry).updateObservedUnauthorisedRequestTimestamp();
    }

    @Test
    public void shouldMakePostRequestToApiMobileWithJsonContentProvidedInRequest() throws Exception {
        fakeApiMobileResponse(new MockResponse());
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.Builder.post(PATH)
                .forPrivateApi(1)
                .withContent(new ApiTrack())
                .build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getMethod()).toEqual("POST");
        expect(new String(recordedRequest.getBody())).toEqual(JSON_DATA);
        expect(recordedRequest.getHeader("Content-Type")).toEqual("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldMakePostRequestToApiMobileWithoutContent() throws Exception {
        fakeApiMobileResponse(new MockResponse());
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.Builder.post(PATH)
                .forPrivateApi(1)
                .build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getMethod()).toEqual("POST");
        expect(new String(recordedRequest.getBody())).toEqual("");
        expect(recordedRequest.getHeader("Content-Type")).toEqual("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldMakePostRequestsToPublicApiWithoutCharsetsInContentType() throws Exception {
        fakePublicApiResponse(new MockResponse());
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.Builder.post(PATH)
                .forPublicApi()
                .withContent(new ApiTrack())
                .build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getMethod()).toEqual("POST");
        expect(new String(recordedRequest.getBody())).toEqual(JSON_DATA);
        expect(recordedRequest.getHeader("Content-Type")).toEqual("application/json");
    }

    @Test
    public void shouldMakePutRequestToApiMobileWithJsonContentProvidedInRequest() throws Exception {
        fakeApiMobileResponse(new MockResponse());
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.Builder.put(PATH)
                .forPrivateApi(1)
                .withContent(new ApiTrack())
                .build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getMethod()).toEqual("PUT");
        expect(new String(recordedRequest.getBody())).toEqual(JSON_DATA);
        expect(recordedRequest.getHeader("Content-Type")).toEqual("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
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
        fakeApiMobileResponse(new MockResponse());
        ApiRequest request = ApiRequest.Builder.delete(PATH)
                .forPrivateApi(1)
                .build();

        ApiResponse response = apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(response.isSuccess()).toBeTrue();
        expect(recordedRequest.getMethod()).toEqual("DELETE");
    }

    @Test
    public void shouldFetchResourcesMappedToTypeSpecifiedInRequest() throws Exception {
        fakeApiMobileResponse(new MockResponse().setBody(JSON_DATA));
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
        fakeApiMobileResponse(new MockResponse().setBody(JSON_DATA));
        when(jsonTransformer.fromJson(JSON_DATA, TypeToken.of(ApiTrack.class))).thenReturn(new UnknownResource());
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(PATH)
                .forPrivateApi(1)
                .forResource(TypeToken.of(ApiTrack.class))
                .build();
        apiClient.fetchMappedResponse(request);
    }

    @Test(expected = ApiMapperException.class)
    public void shouldThrowMappingExceptionIfResponseBodyIsBlank() throws Exception {
        fakeApiMobileResponse(new MockResponse().setBody(""));
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(PATH)
                .forPrivateApi(1)
                .forResource(TypeToken.of(ApiTrack.class))
                .build();
        apiClient.fetchMappedResponse(request);
    }

    @Test(expected = ApiRequestException.class)
    public void shouldThrowMappingExceptionIfResponseWasUnsuccessful() throws Exception {
        fakeApiMobileResponse(new MockResponse().setResponseCode(500));
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(PATH)
                .forPrivateApi(1)
                .forResource(TypeToken.of(ApiTrack.class))
                .build();
        apiClient.fetchMappedResponse(request);
    }

    private void fakeApiMobileResponse(MockResponse... mockResponses) throws IOException {
        for (MockResponse response : mockResponses) {
            mockWebServer.enqueue(response);
        }
        when(httpProperties.getMobileApiBaseUrl()).thenReturn(mockWebServer.getUrl("").toString());
    }

    private void fakePublicApiResponse(MockResponse mockResponse) throws IOException {
        mockWebServer.enqueue(mockResponse);
        when(httpProperties.getPublicApiBaseUrl()).thenReturn(mockWebServer.getUrl("").toString());
    }

}
