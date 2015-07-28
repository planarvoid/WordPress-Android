package com.soundcloud.android.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.MultiMap;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ApiRequestTest extends AndroidUnitTest {
    private static final String URI_PATH = "/somepath";
    private static final String FULL_URI = "http://api.soundcloud.com/somepath?a=1&b=2";

    @Test
    public void shouldReturnRequestInstanceWithURISet() {
        ApiRequest request = validRequest(URI_PATH).build();
        assertThat(request.getEncodedPath()).isEqualTo(URI_PATH);
    }

    @Test
    public void shouldReturnRequestWithCorrectUriPathFromFullUri() {
        ApiRequest request = validRequest(FULL_URI).build();
        assertThat(request.getEncodedPath()).isEqualTo(URI_PATH);
    }

    @Test
    public void shouldReturnRequestInstanceWithGetMethodSet() {
        ApiRequest request = validRequest(URI_PATH).build();
        assertThat(request.getMethod()).isEqualTo("GET");
    }

    @Test
    public void shouldReturnRequestInstanceWithPostMethodSet() {
        ApiRequest request = ApiRequest.post(URI_PATH).forPrivateApi(1).build();
        assertThat(request.getMethod()).isEqualTo("POST");
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
        assertThat(request.getVersion()).isEqualTo(1);
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
        assertThat(request.isPrivate()).isTrue();
    }

    @Test
    public void shouldReturnSpecifiedPublicAPITarget() {
        ApiRequest request = ApiRequest.get(URI_PATH).forPublicApi().build();
        assertThat(request.isPrivate()).isFalse();
    }

    @Test
    public void shouldAddSingleQueryParameterToRequest() {
        ApiRequest request = validRequest(URI_PATH)
                .addQueryParam("key", 1)
                .build();
        assertThat(request.getQueryParameters().get("key")).containsExactly("1");
    }

    @Test
    public void shouldAddMultipleQueryParameterValuesIfKeyMatches() {
        ApiRequest request = validRequest(URI_PATH)
                .addQueryParam("key", 1)
                .addQueryParam("key", 2)
                .build();
        assertThat(request.getQueryParameters().get("key")).containsExactly("1", "2");
    }

    @Test
    public void shouldAddMultipleQueryParameterValuesDirectly() {
        ApiRequest request = validRequest(URI_PATH)
                .addQueryParam("key", 1, 2)
                .build();
        assertThat(request.getQueryParameters().get("key")).containsExactly("1", "2");
    }

    @Test
    public void shouldReturnEmptyQueryParameterMapIfNoParametersSpecified() {
        ApiRequest request = validRequest(URI_PATH).build();
        assertThat(request.getQueryParameters().isEmpty()).isTrue();
    }

    @Test
    public void shouldSetParametersFromFullUri() {
        ApiRequest request = validRequest(FULL_URI).build();
        final MultiMap<String, String> queryParameters = request.getQueryParameters();
        assertThat(queryParameters.get("a")).containsExactly("1");
        assertThat(queryParameters.get("b")).containsExactly("2");
    }

    @Test
    public void shouldDropExistingParametersFromFullUriSoTheyWontEndUpAddedTwice() {
        ApiRequest request = validRequest(FULL_URI).build();
        assertThat(request.getUri().getQuery()).isNull();
    }

    @Test
    public void remembersAddedHeaders() {
        ApiRequest request = validRequest(URI_PATH)
                .withHeader("sc-udid", "abc123")
                .build();

        final Map<String, String> headers = request.getHeaders();
        final String value = headers.get("sc-udid");

        assertThat(value).isEqualTo("abc123");
    }

    @Test
    public void shouldReturnJsonAcceptMediaTypeForMobileApiRequests() {
        final ApiRequest request = ApiRequest.get(URI_PATH).forPrivateApi(1).build();
        assertThat(request.getAcceptMediaType()).isEqualTo("application/vnd.com.soundcloud.mobile.v1+json; charset=utf-8");
    }

    @Test
    public void shouldReturnJsonAcceptMediaTypeForPublicApiRequests() {
        final ApiRequest request = ApiRequest.get(URI_PATH).forPublicApi().build();
        assertThat(request.getAcceptMediaType()).isEqualTo("application/json");
    }

    @Test
    public void shouldCreateObjectContentRequestWhenUsingWithContent() {
        Map<Object, Object> requestContent = new HashMap<>();
        ApiRequest request = validRequest(URI_PATH)
                .withContent(requestContent)
                .build();
        assertThat(request).isInstanceOf(ApiObjectContentRequest.class);
        final ApiObjectContentRequest contentRequest = (ApiObjectContentRequest) request;
        assertThat(contentRequest.getContent()).isEqualTo(requestContent);
    }

    @Test
    public void shouldCreateFileContentRequestWhenUsingWithFile() {
        final FormPart part1 = new FilePart("file1.txt", new File("/path1"), "testFile1", "image/png");
        final FormPart part2 = new StringPart("param", "value");
        ApiRequest request = validRequest(URI_PATH)
                .withFormPart(part1)
                .withFormPart(part2)
                .build();
        assertThat(request).isInstanceOf(ApiMultipartRequest.class);
        final ApiMultipartRequest contentRequest = (ApiMultipartRequest) request;
        assertThat(contentRequest.getParts()).containsExactly(part1, part2);
    }

    private ApiRequest.Builder validRequest(String uri) {
        return ApiRequest.get(uri).forPrivateApi(1);
    }
}
