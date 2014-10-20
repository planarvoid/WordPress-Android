package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ApiRequestTest {
    private static final String URI_PATH = "/somepath";
    private static final String FULL_URI = "http://api.soundcloud.com/somepath?a=1&b=2";


    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBuildRequestIfURIIsNull() throws Exception {
          ApiRequest.Builder.<Integer>get(null).forResource(Integer.class).forPrivateApi(1).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBuildRequestIfURIIsEmptyString() throws Exception {
        ApiRequest.Builder.<Integer>get("   ").forResource(Integer.class).forPublicApi().build();
    }

    @Test
    public void shouldReturnRequestInstanceWithURISet() {
        ApiRequest<Integer> request = buildValidRequest();
        expect(request.getEncodedPath()).toEqual(URI_PATH);
    }

    @Test
    public void shouldReturnRequestWithCorrectUriPathFromFullUri() {
        ApiRequest<Integer> request = buildValidRequestFromFullUri();
        expect(request.getEncodedPath()).toEqual(URI_PATH);
    }

    @Test
    public void shouldReturnRequestInstanceWithGetMethodSet() {
        ApiRequest<Integer> request = buildValidRequest();
        expect(request.getMethod()).toEqual("GET");
    }

    @Test
    public void shouldReturnRequestInstanceWithPostMethodSet() {
        ApiRequest<Integer> request = ApiRequest.Builder.<Integer>post(URI_PATH).forResource(Integer.class).forPrivateApi(1).build();
        expect(request.getMethod()).toEqual("POST");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIllegalArgumentExceptionOnNegativeVersionValue() {
        ApiRequest.Builder.<Integer>get(URI_PATH).forResource(Integer.class).forPrivateApi(-1).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIllegalArgumentExceptionOnZeroVersionValueForPrivateAPI() {
        ApiRequest.Builder.<Integer>get(URI_PATH).forResource(Integer.class).forPrivateApi(0).build();
    }

    @Test
    public void shouldReturnRequestInstanceWithVersionCodeSetForPrivateAPI() {
        ApiRequest<Integer> request = buildValidRequest();
        expect(request.getVersion()).toBe(1);
    }

    @Test
    public void shouldAllowNoVersionForPublicAPI() {
        ApiRequest.Builder.<Integer>get("/uri").forResource(Integer.class).forPublicApi().build();
    }

    @Test
    public void shouldReturnSpecifiedResourceTypeClassForValidRequest(){
        ApiRequest<Integer> request = buildValidRequest();
        expect(request.getResourceType()).toEqual(new TypeToken<Integer>() {
        });
    }

    @Test
    public void shouldReturnSpecifiedResourceTypeTokenForValidRequest(){
        ApiRequest<List<Integer>> request = ApiRequest.Builder.<List<Integer>>get(URI_PATH).
                forResource(new TypeToken<List<Integer>>() {}).forPrivateApi(1).build();
        expect(request.getResourceType()).toEqual(new TypeToken<List<Integer>>(){});
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfAPIModeNotSet(){
        ApiRequest.Builder.<Integer>get(URI_PATH).
                forResource(Integer.class).build();
    }

    @Test
    public void shouldReturnSpecifiedPrivateAPITarget(){
        ApiRequest<Integer> request = buildValidRequest();
        expect(request.isPrivate()).toBeTrue();
    }

    @Test
    public void shouldReturnSpecifiedPublicAPITarget(){
        ApiRequest<Integer> request = ApiRequest.Builder.<Integer>get(URI_PATH).forResource(Integer.class).forPublicApi().build();
        expect(request.isPrivate()).toBeFalse();
    }

    @Test
    public void shouldAddSingleQueryParameterToRequest(){
        ApiRequest<Integer> request = ApiRequest.Builder.<Integer>get(URI_PATH).forResource(Integer.class).forPublicApi().addQueryParameters("key", 1).build();
        expect(request.getQueryParameters().get("key")).toContainExactly("1");
    }

    @Test
    public void shouldAddMultipleQueryParameterToRequest(){
        ApiRequest<Integer> request = ApiRequest.Builder.<Integer>get(URI_PATH).forResource(Integer.class).forPublicApi().addQueryParameters("key", "value1", "value2").build();
        expect(request.getQueryParameters().get("key")).toContainExactly("value1", "value2");
    }

    @Test
    public void shouldReplaceSingleQueryParameterOnRequest(){
        ApiRequest<Integer> request = ApiRequest.Builder.<Integer>get(URI_PATH).forResource(Integer.class).forPublicApi()
                .addQueryParameters("key", "value").addQueryParameters("key", "value2").build();
        expect(request.getQueryParameters().get("key")).toContainExactly("value", "value2");
    }

    @Test
    public void shouldReturnEmptyQueryParameterMapIfNoParametersSpecified(){
        ApiRequest<Integer> request = buildValidRequest();
        expect(request.getQueryParameters()).toEqual(ArrayListMultimap.<String, String>create());
    }

    @Test
    public void shouldReplaceQueryParametersFromCollectionOnRequest(){
        ApiRequest<Integer> request = ApiRequest.Builder.<Integer>get(URI_PATH).forResource(Integer.class).forPublicApi()
                .addQueryParameters("key", "value").addQueryParameters("key", "value2").build();
        expect(request.getQueryParameters().get("key")).toContainExactly("value", "value2");
    }

    @Test
    public void shouldSetParametersFromFullUri() {
        ApiRequest<Integer> request = buildValidRequestFromFullUri();
        final Multimap<String,String> queryParameters = request.getQueryParameters();
        expect(queryParameters.get("a")).toContainExactly("1");
        expect(queryParameters.get("b")).toContainExactly("2");
    }

    @Test
    public void remembersAddedHeaders() {
        ApiRequest<Integer> request = ApiRequest.Builder
                .<Integer>get(URI_PATH)
                .forResource(Integer.class)
                .forPrivateApi(1)
                .withHeader("sc-udid", "abc123")
                .build();

        final String value = request.getHeaders().get("sc-udid");

        expect(value).toEqual("abc123");
    }

    private ApiRequest<Integer> buildValidRequest() {
        return ApiRequest.Builder.<Integer>get(URI_PATH).forResource(Integer.class).forPrivateApi(1).build();
    }

    private ApiRequest<Integer> buildValidRequestFromFullUri() {
        return ApiRequest.Builder.<Integer>get(FULL_URI).forResource(Integer.class).forPrivateApi(1).build();
    }

}
