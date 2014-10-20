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
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import com.soundcloud.api.fakehttp.FakeHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class ApiClientTest {

    private static final String URI = "/uri";
    private static final String JSON_DATA = "{}";

    private ApiClient apiClient;

    @Mock private JsonTransformer jsonTransformer;
    @Mock private ApiWrapperFactory wrapperFactory;
    @Mock private PublicApiWrapper publicApiWrapper;
    @Mock private HttpProperties httpProperties;
    @Captor private ArgumentCaptor<Request> requestCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        apiClient = new ApiClient(httpProperties, jsonTransformer, wrapperFactory);
        when(wrapperFactory.createWrapper(any(ApiRequest.class))).thenReturn(publicApiWrapper);
    }

    @Test
    public void shouldMakeSuccessfulGetRequestWithApiWrapper() throws IOException {
        when(publicApiWrapper.get(any(Request.class))).thenReturn(new FakeHttpResponse(200, "ok"));
        ApiRequest request = ApiRequest.Builder.get(URI).forPublicApi().build();
        ApiResponse response = apiClient.fetchResponse(request);
        expect(response.isSuccess()).toBeTrue();
        expect(response.hasResponseBody()).toBeTrue();
        expect(response.getResponseBody()).toEqual("ok");
    }

    @Test
    public void shouldMakeRequestToGivenUri() throws IOException {
        when(publicApiWrapper.get(requestCaptor.capture())).thenReturn(new FakeHttpResponse(200, "ok"));
        ApiRequest request = ApiRequest.Builder.get(URI).forPublicApi().build();
        apiClient.fetchResponse(request);
        expect(requestCaptor.getValue().toUrl()).toEqual(URI);
    }

    @Test
    public void shouldPrependBasePathWhenMakingRequestsToMobileApi() throws IOException {
        when(httpProperties.getApiMobileBaseUriPath()).thenReturn("/app/mobileapps");
        when(publicApiWrapper.get(requestCaptor.capture())).thenReturn(new FakeHttpResponse(200, "ok"));
        ApiRequest request = ApiRequest.Builder.get(URI).forPrivateApi(1).build();
        apiClient.fetchResponse(request);
        expect(requestCaptor.getValue().toUrl()).toEqual("/app/mobileapps" + URI);
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
    public void shouldMakeRequestWithSingleValueQueryParameter() throws IOException {
        when(publicApiWrapper.get(requestCaptor.capture())).thenReturn(new FakeHttpResponse(200, "ok"));
        ApiRequest request = ApiRequest.Builder.get(ApiClient.URI_APP_PREFIX)
                .forPublicApi()
                .addQueryParameters("key", "value")
                .build();

        apiClient.fetchResponse(request);
        Request wrappedRequest = requestCaptor.getValue();

        expect(wrappedRequest.getParams().get("key")).toEqual("value");
    }

    @Test
    public void shouldFailRequestIfApiReturnsErrorCode() throws IOException {
        when(publicApiWrapper.get(any(Request.class))).thenReturn(new FakeHttpResponse(404, "not found"));
        ApiRequest request = ApiRequest.Builder.get(URI).forPublicApi().build();
        ApiResponse response = apiClient.fetchResponse(request);
        expect(response.isSuccess()).toBeFalse();
        expect(response.getFailure()).not.toBeNull();
        expect(response.getFailure().reason()).toBe(ApiRequestException.Reason.NOT_FOUND);
    }

    @Test
    public void shouldFailRequestIfCallTerminatesWithIoException() throws IOException {
        when(publicApiWrapper.get(any(Request.class))).thenThrow(new IOException());
        ApiRequest request = ApiRequest.Builder.get(URI).forPublicApi().build();
        ApiResponse response = apiClient.fetchResponse(request);
        expect(response.isSuccess()).toBeFalse();
        expect(response.getFailure()).not.toBeNull();
        expect(response.getFailure().reason()).toBe(ApiRequestException.Reason.NETWORK_ERROR);
    }

    @Test
    public void shouldFailRequestIfCallTerminatesWithTokenException() throws IOException {
        when(publicApiWrapper.get(any(Request.class))).thenThrow(new CloudAPI.InvalidTokenException(401, ""));
        ApiRequest request = ApiRequest.Builder.get(URI).forPublicApi().build();
        ApiResponse response = apiClient.fetchResponse(request);
        expect(response.isSuccess()).toBeFalse();
        expect(response.getFailure()).not.toBeNull();
        expect(response.getFailure().reason()).toBe(ApiRequestException.Reason.AUTH_ERROR);
    }

    @Test
    public void shouldMakePostRequestWithJsonContentProvidedInRequest() throws Exception {
        when(publicApiWrapper.post(requestCaptor.capture())).thenReturn(new FakeHttpResponse(200, "ok"));
        when(jsonTransformer.toJson(new ApiTrack())).thenReturn(JSON_DATA);
        ApiRequest request = ApiRequest.Builder.post(URI)
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
    public void shouldFailPostRequestAsMalformedIfContentSerializationFails() throws Exception {
        when(jsonTransformer.toJson(new ApiTrack())).thenThrow(new ApiMapperException("fail"));
        ApiRequest request = ApiRequest.Builder.post(URI)
                .forPublicApi()
                .withContent(new ApiTrack())
                .build();
        ApiResponse response = apiClient.fetchResponse(request);
        expect(response.isSuccess()).toBeFalse();
        expect(response.getFailure().reason()).toBe(ApiRequestException.Reason.MALFORMED_INPUT);
    }

    @Test
    public void shouldFetchResourcesMappedToTypeSpecifiedInRequest() throws Exception {
        final ApiTrack mappedTrack = new ApiTrack();
        when(jsonTransformer.fromJson(JSON_DATA, TypeToken.of(ApiTrack.class))).thenReturn(mappedTrack);
        when(publicApiWrapper.get(any(Request.class))).thenReturn(new FakeHttpResponse(200, JSON_DATA));
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(URI)
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
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(URI)
                .forPublicApi()
                .forResource(TypeToken.of(ApiTrack.class))
                .build();
        apiClient.fetchMappedResponse(request);
    }

    @Test(expected = ApiMapperException.class)
    public void shouldThrowMappingExceptionIfResponseBodyIsBlank() throws Exception {
        when(publicApiWrapper.get(any(Request.class))).thenReturn(new FakeHttpResponse(200, ""));
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(URI)
                .forPublicApi()
                .forResource(TypeToken.of(ApiTrack.class))
                .build();
        apiClient.fetchMappedResponse(request);
    }

    @Test(expected = ApiMapperException.class)
    public void shouldThrowMappingExceptionIfResponseWasUnsuccessful() throws Exception {
        when(publicApiWrapper.get(any(Request.class))).thenReturn(new FakeHttpResponse(400, "bad request"));
        ApiRequest<ApiTrack> request = ApiRequest.Builder.<ApiTrack>get(URI)
                .forPublicApi()
                .forResource(TypeToken.of(ApiTrack.class))
                .build();
        apiClient.fetchMappedResponse(request);
    }
}
