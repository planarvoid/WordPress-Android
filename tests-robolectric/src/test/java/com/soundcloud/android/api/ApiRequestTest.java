package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
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
        ApiRequest request = ApiRequest.Builder.post(URI_PATH).forResource(Object.class).forPrivateApi(1).build();
        expect(request.getMethod()).toEqual("POST");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIllegalArgumentExceptionOnNegativeVersionValue() {
        ApiRequest.Builder.get(URI_PATH).forResource(Object.class).forPrivateApi(-1).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIllegalArgumentExceptionOnZeroVersionValueForPrivateAPI() {
        ApiRequest.Builder.get(URI_PATH).forResource(Object.class).forPrivateApi(0).build();
    }

    @Test
    public void shouldReturnRequestInstanceWithVersionCodeSetForPrivateAPI() {
        ApiRequest request = validRequest(URI_PATH).build();
        expect(request.getVersion()).toBe(1);
    }

    @Test
    public void shouldAllowNoVersionForPublicAPI() {
        ApiRequest.Builder.get("/uri").forResource(Object.class).forPublicApi().build();
    }

    @Test
    public void shouldReturnSpecifiedResourceTypeClassForValidRequest() {
        ApiRequest request = validRequest(URI_PATH).build();
        expect(request.getResourceType()).toEqual(new TypeToken<Object>() {
        });
    }

    @Test
    public void shouldReturnSpecifiedResourceTypeTokenForValidRequest() {
        ApiRequest<List<Object>> request = ApiRequest.Builder.<List<Object>>get(URI_PATH).
                forResource(new TypeToken<List<Object>>() {
                }).forPrivateApi(1).build();
        expect(request.getResourceType()).toEqual(new TypeToken<List<Object>>() {
        });
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfAPIModeNotSet() {
        ApiRequest.Builder.get(URI_PATH).forResource(Object.class).build();
    }

    @Test
    public void shouldReturnSpecifiedPrivateAPITarget() {
        ApiRequest request = validRequest(URI_PATH).build();
        expect(request.isPrivate()).toBeTrue();
    }

    @Test
    public void shouldReturnSpecifiedPublicAPITarget() {
        ApiRequest request = ApiRequest.Builder.get(URI_PATH).forResource(Object.class).forPublicApi().build();
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

    private ApiRequest.Builder validRequest(String uri) {
        return ApiRequest.Builder.get(uri).forResource(Object.class).forPrivateApi(1);
    }
}
