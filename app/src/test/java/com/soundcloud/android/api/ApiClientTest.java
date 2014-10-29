package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.model.UnknownResource;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import com.soundcloud.api.fakehttp.FakeHttpResponse;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class ApiClientTest {

    private static final String PATH = "/path/to/resource";
    private static final String JSON_DATA = "{}";

    private ApiClient apiClient;
    private OkHttpClient httpClient = new OkHttpClient();
    private MockWebServer mockWebServer = new MockWebServer();

    @Mock private FeatureFlags featureFlags;
    @Mock private JsonTransformer jsonTransformer;
    @Mock private ApiWrapperFactory wrapperFactory;
    @Mock private PublicApiWrapper publicApiWrapper;
    @Mock private HttpProperties httpProperties;
    @Mock private DeviceHelper deviceHelper;
    @Captor private ArgumentCaptor<Request> requestCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(wrapperFactory.createWrapper(any(ApiRequest.class))).thenReturn(publicApiWrapper);
        when(deviceHelper.getUserAgent()).thenReturn("");
        apiClient = new ApiClient(featureFlags, httpClient, new ApiUrlBuilder(httpProperties), jsonTransformer, wrapperFactory, deviceHelper);
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void shouldMakeSuccessfulGetRequestWithApiWrapper() throws IOException {
        when(publicApiWrapper.get(any(Request.class))).thenReturn(new FakeHttpResponse(200, "ok"));
        ApiRequest request = ApiRequest.Builder.get(PATH).forPublicApi().build();
        ApiResponse response = apiClient.fetchResponse(request);
        expect(response.isSuccess()).toBeTrue();
        expect(response.hasResponseBody()).toBeTrue();
        expect(response.getResponseBody()).toEqual("ok");
    }

    @Test
    public void shouldMakeSuccessfulGetRequestToPublicApiWithOkHttp() throws Exception {
        fakePublicApiResponse(new MockResponse().setBody("ok"));

        ApiRequest request = ApiRequest.Builder.get(PATH).forPublicApi().build();
        ApiResponse response = apiClient.fetchResponse(request);

        expect(response.isSuccess()).toBeTrue();
        expect(response.hasResponseBody()).toBeTrue();
        expect(response.getResponseBody()).toEqual("ok");
        final RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getMethod()).toEqual("GET");
        expect(recordedRequest.getPath()).toEqual(PATH);
    }

    @Test
    public void shouldMakeSuccessfulGetRequestToMobileApiWithOkHttp() throws Exception {
        fakeApiMobileResponse(new MockResponse().setBody("ok"));

        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).build();
        ApiResponse response = apiClient.fetchResponse(request);

        expect(response.isSuccess()).toBeTrue();
        expect(response.hasResponseBody()).toBeTrue();
        expect(response.getResponseBody()).toEqual("ok");
        final RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getMethod()).toEqual("GET");
        expect(recordedRequest.getPath()).toEqual(PATH);
    }

    @Test
    public void shouldMakeRequestToMobileApiWithAbsoluteUrlAndOkHttp() throws Exception {
        fakeApiMobileResponse(new MockResponse());

        ApiRequest request = ApiRequest.Builder.get(mockWebServer.getUrl(PATH).toString()).
                forPrivateApi(1).build();
        ApiResponse response = apiClient.fetchResponse(request);

        final RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(response.isSuccess()).toBeTrue();
        expect(recordedRequest.getPath()).toEqual(PATH);
    }

    @Test
    public void shouldSendUserAgentHeaderWithOkHttp() throws Exception {
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
    public void shouldRequestGzippedResponsesByDefaultWithOkHttp() throws Exception {
        fakeApiMobileResponse(new MockResponse());

        ApiRequest request = ApiRequest.Builder.get(mockWebServer.getUrl(PATH).toString()).
                forPrivateApi(1).build();
        ApiResponse response = apiClient.fetchResponse(request);

        final RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(response.isSuccess()).toBeTrue();
        expect(recordedRequest.getHeader("Accept-Encoding")).toEqual("gzip");
    }

    @Test
    public void shouldMakeRequestToGivenUriWithApiWrapper() throws IOException {
        when(publicApiWrapper.get(requestCaptor.capture())).thenReturn(new FakeHttpResponse(200, "ok"));
        ApiRequest request = ApiRequest.Builder.get(PATH).forPublicApi().build();
        apiClient.fetchResponse(request);
        expect(requestCaptor.getValue().toUrl()).toEqual(PATH);
    }

    @Test
    public void shouldPrependBasePathWhenMakingRequestsToMobileApi() throws IOException {
        when(httpProperties.getApiMobileBaseUriPath()).thenReturn("/app/mobileapps");
        when(publicApiWrapper.get(requestCaptor.capture())).thenReturn(new FakeHttpResponse(200, "ok"));
        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).build();
        apiClient.fetchResponse(request);
        expect(requestCaptor.getValue().toUrl()).toEqual("/app/mobileapps" + PATH);
    }

    @Test
    public void shouldNotAppendAppPrefixIfAlreadyPresent() throws IOException {
        when(httpProperties.getApiMobileBaseUriPath()).thenReturn("/app/mobileapps");
        when(publicApiWrapper.get(requestCaptor.capture())).thenReturn(new FakeHttpResponse(200, "ok"));
        ApiRequest request = ApiRequest.Builder.get(ApiClient.URI_APP_PREFIX).forPrivateApi(1).build();
        apiClient.fetchResponse(request);
        expect(requestCaptor.getValue().toUrl()).toEqual(ApiClient.URI_APP_PREFIX);
    }

    @Test
    public void shouldMakeRequestWithQueryParameterAndApiWrapper() throws IOException {
        when(publicApiWrapper.get(requestCaptor.capture())).thenReturn(new FakeHttpResponse(200, "ok"));
        ApiRequest request = ApiRequest.Builder.get(PATH)
                .forPublicApi()
                .addQueryParam("key", "value")
                .build();

        apiClient.fetchResponse(request);
        Request wrappedRequest = requestCaptor.getValue();

        expect(wrappedRequest.getParams().get("key")).toEqual("value");
    }

    @Test
    public void shouldMakeRequestWithQueryParameterAndOkHttp() throws Exception {
        fakeApiMobileResponse(new MockResponse());
        ApiRequest request = ApiRequest.Builder.get(PATH)
                .forPrivateApi(1)
                .addQueryParam("k1", "v1")
                .addQueryParam("k2", "v2")
                .build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getPath()).toEqual("/path/to/resource?k1=v1&k2=v2");
    }

    @Test
    public void shouldForwardRequestHeadersUsingOkHttp() throws Exception {
        fakeApiMobileResponse(new MockResponse());
        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).withHeader("key", "value").build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getHeaders("key")).toContainExactly("value");
    }

    @Test
    public void shouldSynthesizeContentTypeHeaderWithVersionForMobileApiRequestsUsingOkHttp() throws Exception {
        fakeApiMobileResponse(new MockResponse());
        ApiRequest request = ApiRequest.Builder.get(PATH).forPrivateApi(1).build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getHeaders("Accept")).toContainExactly("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldSynthesizeAcceptHeaderForPublicApiRequestsUsingOkHttp() throws Exception {
        fakePublicApiResponse(new MockResponse());
        ApiRequest request = ApiRequest.Builder.get(PATH).forPublicApi().build();

        apiClient.fetchResponse(request);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        expect(recordedRequest.getHeaders("Accept")).toContainExactly("application/json");
    }

    @Test
    public void shouldFailRequestIfApiReturnsErrorCode() throws IOException {
        when(publicApiWrapper.get(any(Request.class))).thenReturn(new FakeHttpResponse(404, "not found"));
        ApiRequest request = ApiRequest.Builder.get(PATH).forPublicApi().build();
        ApiResponse response = apiClient.fetchResponse(request);
        expect(response.isSuccess()).toBeFalse();
        expect(response.getFailure()).not.toBeNull();
        expect(response.getFailure().reason()).toBe(ApiRequestException.Reason.NOT_FOUND);
    }

    @Test
    public void shouldFailRequestIfCallTerminatesWithIoException() throws IOException {
        when(publicApiWrapper.get(any(Request.class))).thenThrow(new IOException());
        ApiRequest request = ApiRequest.Builder.get(PATH).forPublicApi().build();
        ApiResponse response = apiClient.fetchResponse(request);
        expect(response.isSuccess()).toBeFalse();
        expect(response.getFailure()).not.toBeNull();
        expect(response.getFailure().reason()).toBe(ApiRequestException.Reason.NETWORK_ERROR);
    }

    @Test
    public void shouldFailRequestIfCallTerminatesWithTokenException() throws IOException {
        when(publicApiWrapper.get(any(Request.class))).thenThrow(new CloudAPI.InvalidTokenException(401, ""));
        ApiRequest request = ApiRequest.Builder.get(PATH).forPublicApi().build();
        ApiResponse response = apiClient.fetchResponse(request);
        expect(response.isSuccess()).toBeFalse();
        expect(response.getFailure()).not.toBeNull();
        expect(response.getFailure().reason()).toBe(ApiRequestException.Reason.AUTH_ERROR);
    }

    @Test
    public void shouldMakePostRequestWithJsonContentProvidedInRequestThroughApiWrapper() throws Exception {
        when(publicApiWrapper.post(requestCaptor.capture())).thenReturn(new FakeHttpResponse(200, "ok"));
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.Builder.post(PATH)
                .forPublicApi()
                .withContent(new ApiTrack())
                .build();
        apiClient.fetchResponse(request);
        final HttpPost wrappedRequest = requestCaptor.getValue().buildRequest(HttpPost.class);
        expect(EntityUtils.toString(wrappedRequest.getEntity())).toEqual(JSON_DATA);
        // do not use MediaType.JSON_UTF8; the public API does not accept qualified media types that include charsets
        expect(wrappedRequest.getFirstHeader("Content-Type").getValue()).toEqual("application/json");
    }

    @Test
    public void shouldMakePostRequestToApiMobileWithJsonContentProvidedInRequestThroughOkHttp() throws Exception {
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
    public void shouldMakePostRequestToApiMobileWithoutContentThroughOkHttp() throws Exception {
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
    public void shouldMakePostRequestsToPublicApiWithoutCharsetsInContentTypeThroughOkHttp() throws Exception {
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
    public void shouldMakePutRequestToApiMobileWithJsonContentProvidedInRequestThroughOkHttp() throws Exception {
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
    public void shouldSendDeleteRequestThroughOkHttp() throws Exception {
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
        final ApiTrack mappedTrack = new ApiTrack();
        when(jsonTransformer.fromJson(JSON_DATA, TypeToken.of(ApiTrack.class))).thenReturn(mappedTrack);
        when(publicApiWrapper.get(any(Request.class))).thenReturn(new FakeHttpResponse(200, JSON_DATA));
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(PATH)
                .forPublicApi()
                .forResource(TypeToken.of(ApiTrack.class))
                .build();
        ApiTrack resource = apiClient.fetchMappedResponse(request);
        expect(resource).toBe(mappedTrack);
    }

    @Test(expected = ApiMapperException.class)
    public void shouldThrowMappingExceptionIfParsedToUnknownResource() throws Exception {
        when(jsonTransformer.fromJson(JSON_DATA, TypeToken.of(ApiTrack.class))).thenReturn(new UnknownResource());
        when(publicApiWrapper.get(any(Request.class))).thenReturn(new FakeHttpResponse(200, JSON_DATA));
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(PATH)
                .forPublicApi()
                .forResource(TypeToken.of(ApiTrack.class))
                .build();
        apiClient.fetchMappedResponse(request);
    }

    @Test(expected = ApiMapperException.class)
    public void shouldThrowMappingExceptionIfResponseBodyIsBlank() throws Exception {
        when(publicApiWrapper.get(any(Request.class))).thenReturn(new FakeHttpResponse(200, ""));
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(PATH)
                .forPublicApi()
                .forResource(TypeToken.of(ApiTrack.class))
                .build();
        apiClient.fetchMappedResponse(request);
    }

    @Test(expected = ApiMapperException.class)
    public void shouldThrowMappingExceptionIfResponseWasUnsuccessful() throws Exception {
        when(publicApiWrapper.get(any(Request.class))).thenReturn(new FakeHttpResponse(400, "bad request"));
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(PATH)
                .forPublicApi()
                .forResource(TypeToken.of(ApiTrack.class))
                .build();
        apiClient.fetchMappedResponse(request);
    }

    private void fakeApiMobileResponse(MockResponse mockResponse) throws IOException {
        when(featureFlags.isEnabled(Feature.OKHTTP)).thenReturn(true);
        mockWebServer.enqueue(mockResponse);
        mockWebServer.play();
        when(httpProperties.getMobileApiBaseUrl()).thenReturn(mockWebServer.getUrl("").toString());
    }

    private void fakePublicApiResponse(MockResponse mockResponse) throws IOException {
        when(featureFlags.isEnabled(Feature.OKHTTP)).thenReturn(true);
        mockWebServer.enqueue(mockResponse);
        mockWebServer.play();
        when(httpProperties.getPublicApiBaseUrl()).thenReturn(mockWebServer.getUrl("").toString());
    }

}
