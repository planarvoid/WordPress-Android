package com.soundcloud.android.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.api.ApiRequest.ProgressListener;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.model.UnknownResource;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestHttpResponses;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.api.legacy.Request;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class ApiClientTest extends AndroidUnitTest {

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
    @Mock private AccountOperations accountOperations;

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
        when(accountOperations.getSoundCloudToken()).thenReturn(new Token("access", "refresh"));
        apiClient = new ApiClient(httpClient, apiUrlBuilder, jsonTransformer,
                deviceHelper, adIdHelper, oAuth, unauthorisedRequestRegistry, accountOperations);
    }

    @Test
    public void shouldMakeSuccessfulHttpRequest() throws Exception {
        ApiRequest request = ApiRequest.get(URL).forPublicApi().build();
        mockSuccessfulResponseFor(request);
        ApiResponse response = apiClient.fetchResponse(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.hasResponseBody()).isTrue();
        assertThat(httpRequestCaptor.getValue().method()).isEqualTo("GET");
        assertThat(httpRequestCaptor.getValue().urlString()).isEqualTo(URL);
    }

    @Test
    public void shouldSendUserAgentHeader() throws Exception {
        when(deviceHelper.getUserAgent()).thenReturn("agent");
        ApiRequest request = ApiRequest.get(URL).forPrivateApi(1).build();
        mockSuccessfulResponseFor(request);

        ApiResponse response = apiClient.fetchResponse(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(httpRequestCaptor.getValue().header("User-Agent")).isEqualTo("agent");
    }

    @Test
    public void shouldAddScalarQueryParametersToHttpRequest() throws Exception {
        ApiRequest request = ApiRequest.get(URL)
                .forPrivateApi(1)
                .addQueryParam("k1", "v1")
                .build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        verify(apiUrlBuilder).withQueryParams(Collections.singletonMap("k1", "v1"));
    }

    @Test
    public void shouldAddMultiDimensionalQueryParametersToHttpRequest() throws Exception {
        ApiRequest request = ApiRequest.get(URL)
                .forPrivateApi(1)
                .addQueryParam("k1", "v1", "v2")
                .build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        verify(apiUrlBuilder).withQueryParams(Collections.singletonMap("k1", "v1,v2"));
    }

    @Test
    public void shouldForwardRequestHeaders() throws Exception {
        ApiRequest request = ApiRequest.get(URL).forPrivateApi(1).withHeader("key", "value").build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().headers("key")).containsExactly("value");
    }

    @Test
    public void shouldSynthesizeContentTypeHeaderWithVersionForMobileApiRequests() throws Exception {
        ApiRequest request = ApiRequest.get(URL).forPrivateApi(1).build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().headers("Accept")).containsExactly("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldSynthesizeAcceptHeaderForPublicApiRequests() throws Exception {
        ApiRequest request = ApiRequest.get(URL).forPublicApi().build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().headers("Accept")).containsExactly("application/json");
    }

    @Test
    public void shouldAddOAuthHeader() throws Exception {
        ApiRequest request = ApiRequest.get(URL).forPrivateApi(1).build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().headers("Authorization")).containsExactly("OAuth 12345");
    }

    @Test
    public void shouldAddUDIDHeaderIfAvailable() throws IOException {
        ApiRequest request = ApiRequest.get(URL).forPrivateApi(1).build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().headers("UDID")).containsExactly("my-udid");
    }

    @Test
    public void shouldOmitUDIDHeaderIfUnavailable() throws IOException {
        when(deviceHelper.hasUdid()).thenReturn(false);
        when(deviceHelper.getUdid()).thenReturn("");
        ApiRequest request = ApiRequest.get(URL).forPrivateApi(1).build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().headers("UDID")).isEmpty();
    }

    @Test
    public void shouldAddAdIdHeadersIfAvailable() throws IOException {
        when(adIdHelper.isAvailable()).thenReturn(true);
        ApiRequest request = ApiRequest.get(URL).forPrivateApi(1).build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().headers("ADID")).containsExactly("my-adid");
        assertThat(httpRequestCaptor.getValue().headers("ADID-TRACKING")).containsExactly("true");
    }

    @Test
    public void shouldOmitAdIdHeadersIfUnvailable() throws IOException {
        when(adIdHelper.isAvailable()).thenReturn(false);
        ApiRequest request = ApiRequest.get(URL).forPrivateApi(1).build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().headers("ADID")).isEmpty();
        assertThat(httpRequestCaptor.getValue().headers("ADID-TRACKING")).isEmpty();
    }

    @Test
    public void shouldRegister401sWithUnauthorizedRequestRegistryWithValidToken() throws IOException {
        when(httpCall.execute()).thenReturn(
                TestHttpResponses.response(200).build(),
                TestHttpResponses.response(401).build());
        ApiRequest request = ApiRequest.get(URL).forPrivateApi(1).build();
        mockRequestBuilderFor(request);
        when(accountOperations.hasValidToken()).thenReturn(true);

        apiClient.fetchResponse(request); // 200 -- no interaction with registry expected
        verifyZeroInteractions(unauthorisedRequestRegistry);
        apiClient.fetchResponse(request); // 401 -- interaction with registry expected
        verify(unauthorisedRequestRegistry).updateObservedUnauthorisedRequestTimestamp();
    }

    @Test
    public void shouldNotRegister401sWithUnauthorizedRequestRegistryWithoutValidToken() throws IOException {
        when(httpCall.execute()).thenReturn(
                TestHttpResponses.response(200).build(),
                TestHttpResponses.response(401).build());
        ApiRequest request = ApiRequest.get(URL).forPrivateApi(1).build();
        mockRequestBuilderFor(request);
        when(accountOperations.hasValidToken()).thenReturn(false);

        apiClient.fetchResponse(request);
        verify(unauthorisedRequestRegistry, never()).updateObservedUnauthorisedRequestTimestamp();
    }

    @Test
    public void shouldMakePostRequestToApiMobileWithJsonContentProvidedInRequest() throws Exception {
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.post(URL)
                .forPrivateApi(1)
                .withContent(new ApiTrack())
                .build();
        mockJsonResponseFor(request, 200, JSON_DATA);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().method()).isEqualTo("POST");
        assertThat(httpRequestCaptor.getValue().body().contentLength()).isEqualTo((long) JSON_DATA.length());
        assertThat(httpRequestCaptor.getValue().body().contentType().toString())
                .isEqualTo("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldMakePostRequestToApiMobileWithoutContent() throws Exception {
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.post(URL)
                .forPrivateApi(1)
                .build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().method()).isEqualTo("POST");
        assertThat(httpRequestCaptor.getValue().body().contentLength()).isEqualTo(0L);
        assertThat(httpRequestCaptor.getValue().body().contentType().toString())
                .isEqualTo("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldMakePutRequestToApiMobileWithJsonContentProvidedInRequest() throws Exception {
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.put(URL)
                .forPrivateApi(1)
                .withContent(new ApiTrack())
                .build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().method()).isEqualTo("PUT");
        assertThat(httpRequestCaptor.getValue().body().contentLength()).isEqualTo((long) JSON_DATA.length());
        assertThat(httpRequestCaptor.getValue().body().contentType().toString())
                .isEqualTo("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldFailPostRequestAsMalformedIfContentSerializationFails() throws Exception {
        when(jsonTransformer.toJson(new ApiTrack())).thenThrow(new ApiMapperException("fail"));
        ApiRequest request = ApiRequest.post(URL)
                .forPublicApi()
                .withContent(new ApiTrack())
                .build();
        mockSuccessfulResponseFor(request);

        ApiResponse response = apiClient.fetchResponse(request);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getFailure().reason()).isEqualTo(ApiRequestException.Reason.MALFORMED_INPUT);
    }

    @Test
    public void shouldMakePostRequestToApiMobileWithMultipartFileContent() throws Exception {
        ApiRequest request = ApiRequest.post(URL)
                .forPrivateApi(1)
                .withFormPart(new StringPart("str", "value"))
                .withFormPart(new FilePart("file1.png", new File("/path"), "file1", "image/png"))
                .build();
        mockJsonResponseFor(request, 200, JSON_DATA);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().method()).isEqualTo("POST");
        assertThat(httpRequestCaptor.getValue().body().contentType().toString()).startsWith(MultipartBuilder.FORM.toString());
    }

    @Test
    public void shouldWrapRequestBodyInProgressRequestBodyWhenProgressListenerSet() throws Exception {
        ProgressListener progressListener = mock(ProgressListener.class);
        ApiRequest request = ApiRequest.post(URL)
                .forPrivateApi(1)
                .withFormPart(new FilePart("file1.png", new File("/path"), "file1", "image/png"))
                .withProgressListener(progressListener)
                .build();
        mockJsonResponseFor(request, 200, JSON_DATA);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().body()).isInstanceOf(ProgressRequestBody.class);
    }

    @Test
    public void shouldSendDeleteRequest() throws Exception {
        ApiRequest request = ApiRequest.delete(URL)
                .forPrivateApi(1)
                .build();
        mockSuccessfulResponseFor(request);
        ApiResponse response = apiClient.fetchResponse(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(httpRequestCaptor.getValue().method()).isEqualTo("DELETE");
    }

    @Test
    public void shouldFetchResourcesMappedToTypeSpecifiedInRequest() throws Exception {
        final ApiTrack mappedTrack = new ApiTrack();
        when(jsonTransformer.fromJson(JSON_DATA, TypeToken.of(ApiTrack.class))).thenReturn(mappedTrack);
        ApiRequest request = ApiRequest.get(URL)
                .forPrivateApi(1)
                .build();
        mockJsonResponseFor(request, 200, JSON_DATA);
        ApiTrack resource = apiClient.fetchMappedResponse(request, ApiTrack.class);
        assertThat(resource).isSameAs(mappedTrack);
    }

    @Test(expected = ApiMapperException.class)
    public void shouldThrowMappingExceptionIfParsedToUnknownResource() throws Exception {
        when(jsonTransformer.fromJson(JSON_DATA, TypeToken.of(ApiTrack.class))).thenReturn(new UnknownResource());
        ApiRequest request = ApiRequest.get(URL)
                .forPrivateApi(1)
                .build();
        mockJsonResponseFor(request, 200, JSON_DATA);
        apiClient.fetchMappedResponse(request, ApiTrack.class);
    }

    @Test(expected = ApiMapperException.class)
    public void shouldThrowMappingExceptionIfResponseBodyIsBlank() throws Exception {
        ApiRequest request = ApiRequest.get(URL)
                .forPrivateApi(1)
                .build();
        mockJsonResponseFor(request, 200, "");
        apiClient.fetchMappedResponse(request, ApiTrack.class);
    }

    @Test(expected = ApiRequestException.class)
    public void shouldThrowMappingExceptionIfResponseWasUnsuccessful() throws Exception {
        ApiRequest request = ApiRequest.get(URL)
                .forPrivateApi(1)
                .build();
        mockJsonResponseFor(request, 500, "");
        apiClient.fetchMappedResponse(request, ApiTrack.class);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowWhenAssertingBackgroundThreadButExecutingOnMainThread() throws Exception {
        apiClient.setAssertBackgroundThread(true);
        apiClient.fetchResponse(ApiRequest.get(URL).forPrivateApi(1).build());
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
