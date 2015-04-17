package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class ApiRequestTest {
    private static final String URI_PATH = "/somepath";
    private static final String FULL_URI = "http://api.soundcloud.com/somepath?a=1&b=2";

    @Test
    public void shouldReturnRequestInstanceWithURISet() {
        ApiRequest request = validRequest(URI_PATH).build();
        expect(request.getEncodedPath()).toEqual(URI_PATH);
    }

    @Test
    public void shouldReturnRequestWithCorrectUriPathFromFullUri() {
        ApiRequest request = validRequest(FULL_URI).build();
        expect(request.getEncodedPath()).toEqual(URI_PATH);
    }

    @Test
    public void shouldReturnRequestInstanceWithGetMethodSet() {
        ApiRequest request = validRequest(URI_PATH).build();
        expect(request.getMethod()).toEqual("GET");
    }

    @Test
    public void shouldReturnRequestInstanceWithPostMethodSet() {
        ApiRequest request = ApiRequest.post(URI_PATH).forPrivateApi(1).build();
        expect(request.getMethod()).toEqual("POST");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIllegalArgumentExceptionOnNegativeVersionValue() {
        ApiRequest.get(URI_PATH).forPrivateApi(-1).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIllegalArgumentExceptionOnZeroVersionValueForPrivateAPI() {
        ApiRequest.get(URI_PATH).forPrivateApi(0).build();
    }

    @Test
    public void shouldReturnRequestInstanceWithVersionCodeSetForPrivateAPI() {
        ApiRequest request = validRequest(URI_PATH).build();
        expect(request.getVersion()).toBe(1);
    }

    @Test
    public void shouldAllowNoVersionForPublicAPI() {
        ApiRequest.get("/uri").forPublicApi().build();
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfAPIModeNotSet() {
        ApiRequest.get(URI_PATH).build();
    }

    @Test
    public void shouldReturnSpecifiedPrivateAPITarget() {
        ApiRequest request = validRequest(URI_PATH).build();
        expect(request.isPrivate()).toBeTrue();
    }

    @Test
    public void shouldReturnSpecifiedPublicAPITarget() {
        ApiRequest request = ApiRequest.get(URI_PATH).forPublicApi().build();
        expect(request.isPrivate()).toBeFalse();
    }

    @Test
    public void shouldAddSingleQueryParameterToRequest() {
        ApiRequest request = validRequest(URI_PATH)
                .addQueryParam("key", 1)
                .build();
        expect(request.getQueryParameters().get("key")).toContainExactly("1");
    }

    @Test
    public void shouldAddMultipleQueryParameterValuesIfKeyMatches() {
        ApiRequest request = validRequest(URI_PATH)
                .addQueryParam("key", 1)
                .addQueryParam("key", 2)
                .build();
        expect(request.getQueryParameters().get("key")).toContainExactly("1", "2");
    }

    @Test
    public void shouldAddMultipleQueryParameterValuesDirectly() {
        ApiRequest request = validRequest(URI_PATH)
                .addQueryParam("key", 1, 2)
                .build();
        expect(request.getQueryParameters().get("key")).toContainExactly("1", "2");
    }

    @Test
    public void shouldReturnEmptyQueryParameterMapIfNoParametersSpecified() {
        ApiRequest request = validRequest(URI_PATH).build();
        expect(request.getQueryParameters()).toEqual(ArrayListMultimap.<String, String>create());
    }

    @Test
    public void shouldSetParametersFromFullUri() {
        ApiRequest request = validRequest(FULL_URI).build();
        final Multimap<String, String> queryParameters = request.getQueryParameters();
        expect(queryParameters.get("a")).toContainExactly("1");
        expect(queryParameters.get("b")).toContainExactly("2");
    }

    @Test
    public void shouldDropExistingParametersFromFullUriSoTheyWontEndUpAddedTwice() {
        ApiRequest request = validRequest(FULL_URI).build();
        expect(request.getUri().getQuery()).toBeNull();
    }

    @Test
    public void remembersAddedHeaders() {
        ApiRequest request = validRequest(URI_PATH)
                .withHeader("sc-udid", "abc123")
                .build();

        final Map<String, String> headers = request.getHeaders();
        final String value = headers.get("sc-udid");

        expect(value).toEqual("abc123");
    }

    @Test
    public void shouldReturnJsonAcceptMediaTypeForMobileApiRequests() {
        final ApiRequest request = ApiRequest.get(URI_PATH).forPrivateApi(1).build();
        expect(request.getAcceptMediaType()).toEqual("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldReturnJsonAcceptMediaTypeForPublicApiRequests() {
        final ApiRequest request = ApiRequest.get(URI_PATH).forPublicApi().build();
        expect(request.getAcceptMediaType()).toEqual("application/json");
    }

    @Test
    public void shouldCreateObjectContentRequestWhenUsingWithContent() {
        Map<Object, Object> requestContent = new HashMap<>();
        ApiRequest request = validRequest(URI_PATH)
                .withContent(requestContent)
                .build();
        expect(request).toBeInstanceOf(ApiObjectContentRequest.class);
        final ApiObjectContentRequest contentRequest = (ApiObjectContentRequest) request;
        expect(contentRequest.getContent()).toEqual(requestContent);
    }

    @Test
    public void shouldCreateFileContentRequestWhenUsingWithFile() {
        File file1 = new File("/path1");
        File file2 = new File("/path2");
        ApiRequest request = validRequest(URI_PATH)
                .withFile(file1, "testFile1", "file1.txt", "text/plain")
                .withFile(file2, "testFile2", "file2.txt", "text/plain; charset=utf-8")
                .build();
        expect(request).toBeInstanceOf(ApiFileContentRequest.class);
        final ApiFileContentRequest contentRequest = (ApiFileContentRequest) request;
        expect(contentRequest.getFiles()).toNumber(2);
        expect(contentRequest.getFiles().get(0).file).toEqual(file1);
        expect(contentRequest.getFiles().get(0).paramName).toEqual("testFile1");
        expect(contentRequest.getFiles().get(0).fileName).toEqual("file1.txt");
        expect(contentRequest.getFiles().get(0).contentType).toEqual("text/plain");
        expect(contentRequest.getFiles().get(1).file).toEqual(file2);
        expect(contentRequest.getFiles().get(1).paramName).toEqual("testFile2");
        expect(contentRequest.getFiles().get(1).fileName).toEqual("file2.txt");
        expect(contentRequest.getFiles().get(1).contentType).toEqual("text/plain; charset=utf-8");
    }

    private ApiRequest.Builder validRequest(String uri) {
        return ApiRequest.get(uri).forPrivateApi(1);
    }
}
