package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.ads.AdIdHelper;
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
import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
public class ApiClientTest {

    private static final String URL = "/path/to/resource";
    private static final String JSON_DATA = "{}";
    private static final String CLIENT_ID = "testClientId";

    private ApiClient apiClient;

    @Mock private OkHttpClient httpClient;
    @Mock private FeatureFlags featureFlags;
    @Mock private JsonTransformer jsonTransformer;
    @Mock private ApiUrlBuilder apiUrlBuilder;
    @Mock private DeviceHelper deviceHelper;
    @Mock private AdIdHelper adIdHelper;
    @Mock private UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    @Mock private OAuth oAuth;
    @Mock private Call httpCall;

    @Captor private ArgumentCaptor<Request> apiRequestCaptor;
    @Captor private ArgumentCaptor<com.squareup.okhttp.Request> httpRequestCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(deviceHelper.getUserAgent()).thenReturn("");
        when(deviceHelper.hasUdid()).thenReturn(true);
        when(deviceHelper.getUdid()).thenReturn("my-udid");
        when(adIdHelper.getAdId()).thenReturn("my-adid");
        when(adIdHelper.getAdIdTracking()).thenReturn(true);
        when(oAuth.getClientId()).thenReturn(CLIENT_ID);
        when(oAuth.getAuthorizationHeaderValue()).thenReturn("OAuth 12345");
        when(httpClient.newCall(httpRequestCaptor.capture())).thenReturn(httpCall);
        apiClient = new ApiClient(httpClient, apiUrlBuilder, jsonTransformer,
                deviceHelper, adIdHelper, oAuth, unauthorisedRequestRegistry);
    }

    @Test
    public void shouldMakeSuccessfulHttpRequest() throws Exception {
        ApiRequest request = ApiRequest.Builder.get(URL).forPublicApi().build();
        mockSuccessfulResponseFor(request);
        ApiResponse response = apiClient.fetchResponse(request);

        expect(response.isSuccess()).toBeTrue();
        expect(response.hasResponseBody()).toBeTrue();
        expect(httpRequestCaptor.getValue().method()).toEqual("GET");
        expect(httpRequestCaptor.getValue().urlString()).toEqual(URL);
    }

    @Test
    public void shouldSendUserAgentHeader() throws Exception {
        when(deviceHelper.getUserAgent()).thenReturn("agent");
        ApiRequest request = ApiRequest.Builder.get(URL).forPrivateApi(1).build();
        mockSuccessfulResponseFor(request);

        ApiResponse response = apiClient.fetchResponse(request);

        expect(response.isSuccess()).toBeTrue();
        expect(httpRequestCaptor.getValue().header("User-Agent")).toEqual("agent");
    }

    @Test
    public void shouldAddScalarQueryParametersToHttpRequest() throws Exception {
        ApiRequest request = ApiRequest.Builder.get(URL)
                .forPrivateApi(1)
                .addQueryParam("k1", "v1")
                .build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        verify(apiUrlBuilder).withQueryParams(Collections.singletonMap("k1", "v1"));
    }

    @Test
    public void shouldAddMultiDimensionalQueryParametersToHttpRequest() throws Exception {
        ApiRequest request = ApiRequest.Builder.get(URL)
                .forPrivateApi(1)
                .addQueryParam("k1", "v1", "v2")
                .build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        verify(apiUrlBuilder).withQueryParams(Collections.singletonMap("k1", "v1,v2"));
    }

    @Test
    public void shouldForwardRequestHeaders() throws Exception {
        ApiRequest request = ApiRequest.Builder.get(URL).forPrivateApi(1).withHeader("key", "value").build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().headers("key")).toContainExactly("value");
    }

    @Test
    public void shouldSynthesizeContentTypeHeaderWithVersionForMobileApiRequests() throws Exception {
        ApiRequest request = ApiRequest.Builder.get(URL).forPrivateApi(1).build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().headers("Accept")).toContainExactly("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldSynthesizeAcceptHeaderForPublicApiRequests() throws Exception {
        ApiRequest request = ApiRequest.Builder.get(URL).forPublicApi().build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().headers("Accept")).toContainExactly("application/json");
    }

    @Test
    public void shouldAddOAuthHeader() throws Exception {
        ApiRequest request = ApiRequest.Builder.get(URL).forPrivateApi(1).build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().headers("Authorization")).toContainExactly("OAuth 12345");
    }

    @Test
    public void shouldAddUDIDHeaderIfAvailable() throws IOException {
        ApiRequest request = ApiRequest.Builder.get(URL).forPrivateApi(1).build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().headers("UDID")).toContainExactly("my-udid");
    }

    @Test
    public void shouldOmitUDIDHeaderIfUnavailable() throws IOException {
        when(deviceHelper.hasUdid()).thenReturn(false);
        when(deviceHelper.getUdid()).thenReturn("");
        ApiRequest request = ApiRequest.Builder.get(URL).forPrivateApi(1).build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().headers("UDID")).toBeEmpty();
    }

    @Test
    public void shouldAddAdIdHeadersIfAvailable() throws IOException {
        when(adIdHelper.isAvailable()).thenReturn(true);
        ApiRequest request = ApiRequest.Builder.get(URL).forPrivateApi(1).build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().headers("ADID")).toContainExactly("my-adid");
        expect(httpRequestCaptor.getValue().headers("ADID-TRACKING")).toContainExactly("true");
    }

    @Test
    public void shouldOmitAdIdHeadersIfUnvailable() throws IOException {
        when(adIdHelper.isAvailable()).thenReturn(false);
        ApiRequest request = ApiRequest.Builder.get(URL).forPrivateApi(1).build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().headers("ADID")).toBeEmpty();
        expect(httpRequestCaptor.getValue().headers("ADID-TRACKING")).toBeEmpty();
    }

    @Test
    public void shouldRegister401sWithUnauthorizedRequestRegistry() throws IOException {
        when(httpCall.execute()).thenReturn(
                TestHttpResponses.response(200).build(),
                TestHttpResponses.response(401).build());
        ApiRequest request = ApiRequest.Builder.get(URL).forPrivateApi(1).build();
        mockRequestBuilderFor(request);

        apiClient.fetchResponse(request); // 200 -- no interaction with registry expected
        verifyZeroInteractions(unauthorisedRequestRegistry);
        apiClient.fetchResponse(request); // 401 -- interaction with registry expected
        verify(unauthorisedRequestRegistry).updateObservedUnauthorisedRequestTimestamp();
    }

    @Test
    public void shouldMakePostRequestToApiMobileWithJsonContentProvidedInRequest() throws Exception {
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.Builder.post(URL)
                .forPrivateApi(1)
                .withContent(new ApiTrack())
                .build();
        mockJsonResponseFor(request, 200, JSON_DATA);

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().method()).toEqual("POST");
        expect(httpRequestCaptor.getValue().body().contentLength()).toEqual((long) JSON_DATA.length());
        expect(httpRequestCaptor.getValue().body().contentType().toString()).toEqual("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldMakePostRequestToApiMobileWithoutContent() throws Exception {
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.Builder.post(URL)
                .forPrivateApi(1)
                .build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().method()).toEqual("POST");
        expect(httpRequestCaptor.getValue().body().contentLength()).toEqual(0L);
        expect(httpRequestCaptor.getValue().body().contentType().toString()).toEqual("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldMakePostRequestsToPublicApiWithoutCharsetsInContentType() throws Exception {
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.Builder.post(URL)
                .forPublicApi()
                .withContent(new ApiTrack())
                .build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().method()).toEqual("POST");
        expect(httpRequestCaptor.getValue().body().contentLength()).toEqual((long) JSON_DATA.length());
        expect(httpRequestCaptor.getValue().body().contentType().toString()).toEqual("application/json");
    }

    @Test
    public void shouldMakePutRequestToApiMobileWithJsonContentProvidedInRequest() throws Exception {
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.Builder.put(URL)
                .forPrivateApi(1)
                .withContent(new ApiTrack())
                .build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        expect(httpRequestCaptor.getValue().method()).toEqual("PUT");
        expect(httpRequestCaptor.getValue().body().contentLength()).toEqual((long) JSON_DATA.length());
        expect(httpRequestCaptor.getValue().body().contentType().toString()).toEqual("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldFailPostRequestAsMalformedIfContentSerializationFails() throws Exception {
        when(jsonTransformer.toJson(new ApiTrack())).thenThrow(new ApiMapperException("fail"));
        ApiRequest request = ApiRequest.Builder.post(URL)
                .forPublicApi()
                .withContent(new ApiTrack())
                .build();
        mockSuccessfulResponseFor(request);

        ApiResponse response = apiClient.fetchResponse(request);
        expect(response.isSuccess()).toBeFalse();
        expect(response.getFailure().reason()).toBe(ApiRequestException.Reason.MALFORMED_INPUT);
    }

    @Test
    public void shouldSendDeleteRequest() throws Exception {
        ApiRequest request = ApiRequest.Builder.delete(URL)
                .forPrivateApi(1)
                .build();
        mockSuccessfulResponseFor(request);
        ApiResponse response = apiClient.fetchResponse(request);

        expect(response.isSuccess()).toBeTrue();
        expect(httpRequestCaptor.getValue().method()).toEqual("DELETE");
    }

    @Test
    public void shouldFetchResourcesMappedToTypeSpecifiedInRequest() throws Exception {
        final ApiTrack mappedTrack = new ApiTrack();
        when(jsonTransformer.fromJson(JSON_DATA, TypeToken.of(ApiTrack.class))).thenReturn(mappedTrack);
        ApiRequest request = ApiRequest.Builder.get(URL)
                .forPrivateApi(1)
                .build();
        mockJsonResponseFor(request, 200, JSON_DATA);
        ApiTrack resource = apiClient.fetchMappedResponse(request, ApiTrack.class);
        expect(resource).toBe(mappedTrack);
    }

    @Test(expected = ApiMapperException.class)
    public void shouldThrowMappingExceptionIfParsedToUnknownResource() throws Exception {
        when(jsonTransformer.fromJson(JSON_DATA, TypeToken.of(ApiTrack.class))).thenReturn(new UnknownResource());
        ApiRequest request = ApiRequest.Builder.get(URL)
                .forPrivateApi(1)
                .build();
        mockJsonResponseFor(request, 200, JSON_DATA);
        apiClient.fetchMappedResponse(request, ApiTrack.class);
    }

    @Test(expected = ApiMapperException.class)
    public void shouldThrowMappingExceptionIfResponseBodyIsBlank() throws Exception {
        ApiRequest request = ApiRequest.Builder.get(URL)
                .forPrivateApi(1)
                .build();
        mockJsonResponseFor(request, 200, "");
        apiClient.fetchMappedResponse(request, ApiTrack.class);
    }

    @Test(expected = ApiRequestException.class)
    public void shouldThrowMappingExceptionIfResponseWasUnsuccessful() throws Exception {
        ApiRequest request = ApiRequest.Builder.get(URL)
                .forPrivateApi(1)
                .build();
        mockJsonResponseFor(request, 500, "");
        apiClient.fetchMappedResponse(request, ApiTrack.class);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowWhenAssertingBackgroundThreadButExecutingOnMainThread() throws Exception {
        apiClient.setAssertBackgroundThread(true);
        apiClient.fetchResponse(ApiRequest.Builder.get(URL).forPrivateApi(1).build());
    }

    private void mockSuccessfulResponseFor(ApiRequest request) throws IOException {
        mockRequestBuilderFor(request);
        when(httpCall.execute()).thenReturn(TestHttpResponses.response(200).build());
    }

    private void mockJsonResponseFor(ApiRequest request, int code, String jsonBody) throws IOException {
        mockRequestBuilderFor(request);
        when(httpCall.execute()).thenReturn(TestHttpResponses.jsonResponse(code, jsonBody).build());
    }

    private void mockRequestBuilderFor(ApiRequest request) {
        when(apiUrlBuilder.from(request)).thenReturn(apiUrlBuilder);
        when(apiUrlBuilder.withQueryParams(anyMap())).thenReturn(apiUrlBuilder);
        when(apiUrlBuilder.build()).thenReturn(request.getUri().toString());
    }

}
