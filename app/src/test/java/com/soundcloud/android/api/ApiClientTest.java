package com.soundcloud.android.api;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.api.ApiRequest.ProgressListener;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.experiments.Assignment;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.configuration.experiments.Layer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestHttpResponses;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.LocaleFormatter;
import com.soundcloud.java.collections.ListMultiMap;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.MultiMap;
import com.soundcloud.java.net.HttpHeaders;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import okhttp3.Call;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ApiClientTest extends AndroidUnitTest {

    private static final String URL = "http://path/to/resource";
    private static final String JSON_DATA = "{}";
    private static final Layer FIRST_LAYER = new Layer("firstLayerName", 123, "firstExperimentName", 456, "firstVariantName");
    private static final Layer SECOND_LAYER = new Layer("secondLayerName", 789, "secondExperimentName", 101, "secondVariantName");
    private static final List<Layer> SINGLE_LAYER = Lists.newArrayList(FIRST_LAYER);
    private static final List<Layer> MULTIPLE_LAYERS = Lists.newArrayList(FIRST_LAYER, SECOND_LAYER);
    private static final String SINGLE_VARIANT_IDS = "456";
    private static final String MULTIPLE_VARIANT_IDS = "456,101";
    private static final String CLIENT_ID = "testClientId";
    private static final String OAUTH_TOKEN = "OAuth 12345";

    private ApiClient apiClient;

    @Mock private OkHttpClient httpClient;
    @Mock private JsonTransformer jsonTransformer;
    @Mock private ApiUrlBuilder apiUrlBuilder;
    @Mock private DeviceHelper deviceHelper;
    @Mock private AdIdHelper adIdHelper;
    @Mock private UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    @Mock private OAuth oAuth;
    @Mock private Call httpCall;
    @Mock private AccountOperations accountOperations;
    @Mock private LocaleFormatter localeFormatter;
    @Mock private ExperimentOperations experimentOperations;

    @Captor private ArgumentCaptor<Request> httpRequestCaptor;

    @Before
    public void setUp() throws Exception {
        when(deviceHelper.getUserAgent()).thenReturn("");
        when(deviceHelper.getUdid()).thenReturn("my-udid");
        when(adIdHelper.getAdId()).thenReturn(Optional.of("my-adid"));
        when(adIdHelper.getAdIdTracking()).thenReturn(true);
        when(oAuth.getClientId()).thenReturn(CLIENT_ID);
        when(oAuth.getAuthorizationHeaderValue()).thenReturn(OAUTH_TOKEN);
        when(httpClient.newCall(httpRequestCaptor.capture())).thenReturn(httpCall);
        when(accountOperations.getSoundCloudToken()).thenReturn(new Token("access", "refresh"));
        when(localeFormatter.getLocale()).thenReturn(Optional.of("fr-CA"));
        apiClient = getApiClient(false);
    }

    @Test
    public void shouldMakeSuccessfulHttpRequest() throws Exception {
        ApiRequest request = ApiRequest.get(URL).forPublicApi().build();
        mockSuccessfulResponseFor(request);
        ApiResponse response = apiClient.fetchResponse(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.hasResponseBody()).isTrue();
        assertThat(httpRequestCaptor.getValue().method()).isEqualTo("GET");
        assertThat(httpRequestCaptor.getValue().url().toString()).isEqualTo(URL);
    }

    @Test
    public void shouldSendUserAgentHeader() throws Exception {
        when(deviceHelper.getUserAgent()).thenReturn("agent");
        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
        mockSuccessfulResponseFor(request);

        ApiResponse response = apiClient.fetchResponse(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(httpRequestCaptor.getValue().header("User-Agent")).isEqualTo("agent");
    }

    @Test
    public void shouldSendAppVersionHeader() throws Exception {
        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
        mockSuccessfulResponseFor(request);

        ApiResponse response = apiClient.fetchResponse(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(httpRequestCaptor.getValue().header("App-Version")).matches("\\d+");
    }

    @Test
    public void shouldSendAppEnvironmentHeader() throws Exception {
        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
        mockSuccessfulResponseFor(request);

        ApiResponse response = apiClient.fetchResponse(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(httpRequestCaptor.getValue().header("App-Environment")).matches("(dev|alpha|beta|prod)");
    }

    @Test
    public void shouldSendDeviceLocaleHeader() throws Exception {
        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
        mockSuccessfulResponseFor(request);

        ApiResponse response = apiClient.fetchResponse(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(httpRequestCaptor.getValue().header("Device-Locale")).isEqualTo("fr-CA");
    }

    @Test
    public void shouldOmitDeviceLocaleHeaderIfLocaleUnavailable() throws Exception {
        when(localeFormatter.getLocale()).thenReturn(Optional.absent());

        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
        mockSuccessfulResponseFor(request);

        ApiResponse response = apiClient.fetchResponse(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(httpRequestCaptor.getValue().header("Device-Locale")).isNull();
    }

    @Test
    public void shouldSendCommaSeparatedAppVariantIdsHeaderWhenUserIsInMultipleExperiments() throws Exception {
        when(experimentOperations.getAssignment()).thenReturn(new Assignment(MULTIPLE_LAYERS));
        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
        mockSuccessfulResponseFor(request);

        ApiResponse response = apiClient.fetchResponse(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(httpRequestCaptor.getValue().header("App-Variant-Ids")).isEqualTo(MULTIPLE_VARIANT_IDS);
    }

    @Test
    public void shouldSendSingleAppVariantIdsHeaderWhenUserIsInSingleExperiment() throws Exception {
        when(experimentOperations.getAssignment()).thenReturn(new Assignment(SINGLE_LAYER));
        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
        mockSuccessfulResponseFor(request);

        ApiResponse response = apiClient.fetchResponse(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(httpRequestCaptor.getValue().header("App-Variant-Ids")).isEqualTo(SINGLE_VARIANT_IDS);
    }

    @Test
    public void shouldNotSendAppVariantIdsHeaderWhenUserIsNotInAnExperiment() throws Exception {
        when(experimentOperations.getAssignment()).thenReturn(Assignment.empty());

        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
        mockSuccessfulResponseFor(request);

        ApiResponse response = apiClient.fetchResponse(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(httpRequestCaptor.getValue().header("App-Variant-Ids")).isNull();
    }

    @Test
    public void shouldAddScalarQueryParametersToHttpRequest() throws Exception {
        ApiRequest request = ApiRequest.get(URL)
                                       .forPrivateApi()
                                       .addQueryParam("k1", "v1")
                                       .build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        verify(apiUrlBuilder).withQueryParams(new ListMultiMap<>(Collections.singletonMap("k1", asList("v1"))));
    }

    @Test
    public void shouldAddMultiDimensionalQueryParametersToHttpRequest() throws Exception {
        ApiRequest request = ApiRequest.get(URL)
                                       .forPrivateApi()
                                       .addQueryParam("k1", "v1", "v2")
                                       .build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        verify(apiUrlBuilder).withQueryParams(new ListMultiMap<>(Collections.singletonMap("k1", asList("v1", "v2"))));
    }

    @Test
    public void shouldForwardRequestHeaders() throws Exception {
        ApiRequest request = ApiRequest.get(URL).forPrivateApi().withHeader("key", "value").build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().headers("key")).containsExactly("value");
    }

    @Test
    public void shouldSynthesizeContentTypeHeaderWithVersionForMobileApiRequests() throws Exception {
        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().headers("Accept")).containsExactly("application/json; charset=utf-8");
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
        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().headers("Authorization")).containsExactly(OAUTH_TOKEN);
    }

    @Test
    public void shouldNotAddAuthorizationOrUDIDIOrADIDRelatedHeadersWhenEndpointIsAnonymous() throws Exception {
        ApiRequest request = ApiRequest.get(ApiEndpoints.SEARCH_AUTOCOMPLETE).forPrivateApi().build();
        mockSuccessfulResponseFor(request);
        when(apiUrlBuilder.build()).thenReturn(URL);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().headers(HttpHeaders.AUTHORIZATION)).isEmpty();
        assertThat(httpRequestCaptor.getValue().headers(ApiHeaders.UDID)).isEmpty();
        assertThat(httpRequestCaptor.getValue().headers(ApiHeaders.ADID)).isEmpty();
        assertThat(httpRequestCaptor.getValue().headers(ApiHeaders.ADID_TRACKING)).isEmpty();
    }

    @Test
    public void shouldAddUDIDHeaderIfAvailable() throws IOException {
        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().headers("UDID")).containsExactly("my-udid");
    }

    @Test
    public void shouldAddAdIdHeadersIfAvailable() throws IOException {
        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().headers("ADID")).containsExactly("my-adid");
        assertThat(httpRequestCaptor.getValue().headers("ADID-TRACKING")).containsExactly("true");
    }

    @Test
    public void shouldOmitAdIdHeadersIfUnvailable() throws IOException {
        when(adIdHelper.getAdId()).thenReturn(Optional.absent());
        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
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
        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
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
        ApiRequest request = ApiRequest.get(URL).forPrivateApi().build();
        mockRequestBuilderFor(request);
        when(accountOperations.hasValidToken()).thenReturn(false);

        apiClient.fetchResponse(request);
        verify(unauthorisedRequestRegistry, never()).updateObservedUnauthorisedRequestTimestamp();
    }

    @Test
    public void shouldMakePostRequestToApiMobileWithJsonContentProvidedInRequest() throws Exception {
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.post(URL)
                                       .forPrivateApi()
                                       .withContent(new ApiTrack())
                                       .build();
        mockJsonResponseFor(request, 200, JSON_DATA);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().method()).isEqualTo("POST");
        assertThat(httpRequestCaptor.getValue().body().contentLength()).isEqualTo((long) JSON_DATA.length());
        assertThat(httpRequestCaptor.getValue().body().contentType().toString())
                .isEqualTo("application/json; charset=utf-8");
    }

    @Test
    public void shouldMakePostRequestToApiMobileWithoutContent() throws Exception {
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.post(URL)
                                       .forPrivateApi()
                                       .build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().method()).isEqualTo("POST");
        assertThat(httpRequestCaptor.getValue().body().contentLength()).isEqualTo(0L);
        assertThat(httpRequestCaptor.getValue().body().contentType().toString())
                .isEqualTo("application/json; charset=utf-8");
    }

    @Test
    public void shouldMakePutRequestToApiMobileWithJsonContentProvidedInRequest() throws Exception {
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.put(URL)
                                       .forPrivateApi()
                                       .withContent(new ApiTrack())
                                       .build();
        mockSuccessfulResponseFor(request);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().method()).isEqualTo("PUT");
        assertThat(httpRequestCaptor.getValue().body().contentLength()).isEqualTo((long) JSON_DATA.length());
        assertThat(httpRequestCaptor.getValue().body().contentType().toString())
                .isEqualTo("application/json; charset=utf-8");
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
                                       .forPrivateApi()
                                       .withFormPart(new StringPart("str", "value"))
                                       .withFormPart(new FilePart("file1.png", new File("/path"), "file1", "image/png"))
                                       .build();
        mockJsonResponseFor(request, 200, JSON_DATA);

        apiClient.fetchResponse(request);

        assertThat(httpRequestCaptor.getValue().method()).isEqualTo("POST");
        assertThat(httpRequestCaptor.getValue()
                                    .body()
                                    .contentType()
                                    .toString()).startsWith(MultipartBody.FORM.toString());
    }

    @Test
    public void shouldWrapRequestBodyInProgressRequestBodyWhenProgressListenerSet() throws Exception {
        ProgressListener progressListener = mock(ProgressListener.class);
        ApiRequest request = ApiRequest.post(URL)
                                       .forPrivateApi()
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
                                       .forPrivateApi()
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
                                       .forPrivateApi()
                                       .build();
        mockJsonResponseFor(request, 200, JSON_DATA);
        ApiTrack resource = apiClient.fetchMappedResponse(request, ApiTrack.class);
        assertThat(resource).isSameAs(mappedTrack);
    }

    @Test(expected = ApiMapperException.class)
    public void shouldThrowMappingExceptionIfParsedToNull() throws Exception {
        when(jsonTransformer.fromJson(eq(JSON_DATA), any(TypeToken.class))).thenReturn(null);
        ApiRequest request = ApiRequest.get(URL)
                                       .forPrivateApi()
                                       .build();
        mockJsonResponseFor(request, 200, JSON_DATA);
        apiClient.fetchMappedResponse(request, ApiTrack.class);
    }

    @Test(expected = RuntimeException.class)
    public void shouldFailFastOnMappingExceptionIfParsedToUnknownResource() throws Exception {
        apiClient = getApiClient(true);

        when(jsonTransformer.fromJson(eq(JSON_DATA), any(TypeToken.class))).thenReturn(null);
        ApiRequest request = ApiRequest.get(URL)
                                       .forPrivateApi()
                                       .build();
        apiClient.fetchMappedResponse(request, ApiTrack.class);
    }

    @Test(expected = ApiMapperException.class)
    public void shouldThrowMappingExceptionIfResponseBodyIsBlank() throws Exception {
        ApiRequest request = ApiRequest.get(URL)
                                       .forPrivateApi()
                                       .build();
        mockJsonResponseFor(request, 200, "");
        apiClient.fetchMappedResponse(request, ApiTrack.class);
    }

    @Test(expected = ApiRequestException.class)
    public void shouldThrowMappingExceptionIfResponseWasUnsuccessful() throws Exception {
        ApiRequest request = ApiRequest.get(URL)
                                       .forPrivateApi()
                                       .build();
        mockJsonResponseFor(request, 500, "");
        apiClient.fetchMappedResponse(request, ApiTrack.class);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowWhenAssertingBackgroundThreadButExecutingOnMainThread() throws Exception {
        apiClient.setAssertBackgroundThread(true);
        apiClient.fetchResponse(ApiRequest.get(URL).forPrivateApi().build());
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
        when(apiUrlBuilder.withQueryParams(any(MultiMap.class))).thenReturn(apiUrlBuilder);
        when(apiUrlBuilder.build()).thenReturn(request.getUri().toString());
    }

    private ApiClient getApiClient(boolean failFastOnMapper) {
        return new ApiClient(httpClient,
                             apiUrlBuilder,
                             jsonTransformer,
                             deviceHelper,
                             adIdHelper,
                             oAuth,
                             unauthorisedRequestRegistry,
                             accountOperations,
                             localeFormatter,
                             failFastOnMapper,
                             experimentOperations);
    }

}
