package com.soundcloud.android.api.http;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.api.http.SoundCloudAPIRequest.RequestBuilder;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class RequestBuilderTest {
    private static final String URI_PATH = "/somepath";
    private static final String FULL_URI = "http://api.soundcloud.com/somepath?a=1&b=2";


    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBuildRequestIfURIIsNull() throws Exception {
          RequestBuilder.<Integer>get(null).forResource(Integer.class).forPrivateAPI(1).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBuildRequestIfURIIsEmptyString() throws Exception {
        RequestBuilder.<Integer>get("   ").forResource(Integer.class).forPublicAPI().build();
    }

    @Test
    public void shouldReturnRequestInstanceWithURISet() {
        APIRequest<Integer> request = buildValidRequest();
        expect(request.getUriPath()).toEqual(URI_PATH);
    }

    @Test
    public void shouldReturnRequestWithCorrectUriPathFromFullUri() {
        APIRequest<Integer> request = buildValidRequestFromFullUri();
        expect(request.getUriPath()).toEqual(URI_PATH);
    }

    @Test
    public void shouldReturnRequestInstanceWithGetMethodSet() {
        APIRequest<Integer> request = buildValidRequest();
        expect(request.getMethod()).toEqual("GET");
    }

    @Test
    public void shouldReturnRequestInstanceWithPostMethodSet() {
        APIRequest<Integer> request = RequestBuilder.<Integer>post(URI_PATH).forResource(Integer.class).forPrivateAPI(1).build();
        expect(request.getMethod()).toEqual("POST");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIllegalArgumentExceptionOnNegativeVersionValue() {
        RequestBuilder.<Integer>get(URI_PATH).forResource(Integer.class).forPrivateAPI(-1).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIllegalArgumentExceptionOnZeroVersionValueForPrivateAPI() {
        RequestBuilder.<Integer>get(URI_PATH).forResource(Integer.class).forPrivateAPI(0).build();
    }

    @Test
    public void shouldReturnRequestInstanceWithVersionCodeSetForPrivateAPI() {
        APIRequest<Integer> request = buildValidRequest();
        expect(request.getVersion()).toBe(1);
    }

    @Test
    public void shouldAllowNoVersionForPublicAPI() {
        RequestBuilder.<Integer>get("/uri").forResource(Integer.class).forPublicAPI().build();
    }

    @Test
    public void shouldReturnSpecifiedResourceTypeClassForValidRequest(){
        APIRequest<Integer> request = buildValidRequest();
        expect(request.getResourceType()).toEqual(new TypeToken<Integer>() {
        });
    }

    @Test
    public void shouldReturnSpecifiedResourceTypeTokenForValidRequest(){
        APIRequest<List<Integer>> request = RequestBuilder.<List<Integer>>get(URI_PATH).
                forResource(new TypeToken<List<Integer>>() {}).forPrivateAPI(1).build();
        expect(request.getResourceType()).toEqual(new TypeToken<List<Integer>>(){});
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfAPIModeNotSet(){
        RequestBuilder.<Integer>get(URI_PATH).
                forResource(Integer.class).build();
    }

    @Test
    public void shouldReturnSpecifiedPrivateAPITarget(){
        APIRequest<Integer> request = buildValidRequest();
        expect(request.isPrivate()).toBeTrue();
    }

    @Test
    public void shouldReturnSpecifiedPublicAPITarget(){
        APIRequest<Integer> request = RequestBuilder.<Integer>get(URI_PATH).forResource(Integer.class).forPublicAPI().build();
        expect(request.isPrivate()).toBeFalse();
    }

    @Test
    public void shouldAddSingleQueryParameterToRequest(){
        APIRequest<Integer> request = RequestBuilder.<Integer>get(URI_PATH).forResource(Integer.class).forPublicAPI().addQueryParameters("key", 1).build();
        expect(request.getQueryParameters().get("key")).toContainExactly("1");
    }

    @Test
    public void shouldAddMultipleQueryParameterToRequest(){
        APIRequest<Integer> request = RequestBuilder.<Integer>get(URI_PATH).forResource(Integer.class).forPublicAPI().addQueryParameters("key", "value1", "value2").build();
        expect(request.getQueryParameters().get("key")).toContainExactly("value1", "value2");
    }

    @Test
    public void shouldReplaceSingleQueryParameterOnRequest(){
        APIRequest<Integer> request = RequestBuilder.<Integer>get(URI_PATH).forResource(Integer.class).forPublicAPI()
                .addQueryParameters("key", "value").addQueryParameters("key", "value2").build();
        expect(request.getQueryParameters().get("key")).toContainExactly("value", "value2");
    }

    @Test
    public void shouldReturnEmptyQueryParameterMapIfNoParametersSpecified(){
        APIRequest<Integer> request = buildValidRequest();
        expect(request.getQueryParameters()).toEqual(ArrayListMultimap.<String, String>create());
    }

    @Test
    public void shouldAddQueryParametersFromCollection(){
        APIRequest<Integer> request = RequestBuilder.<Integer>get(URI_PATH).forResource(Integer.class).forPublicAPI().addQueryParametersAsCollection("key", Lists.newArrayList(1,2)).build();
        expect(request.getQueryParameters().get("key")).toContainExactly("1", "2");
    }

    @Test
    public void shouldReplaceQueryParametersFromCollectionOnRequest(){
        APIRequest<Integer> request = RequestBuilder.<Integer>get(URI_PATH).forResource(Integer.class).forPublicAPI()
                .addQueryParameters("key", "value").addQueryParameters("key", "value2").build();
        expect(request.getQueryParameters().get("key")).toContainExactly("value", "value2");
    }

    @Test
    public void shouldSetParametersFromFullUri() {
        APIRequest<Integer> request = buildValidRequestFromFullUri();
        final Multimap<String,String> queryParameters = request.getQueryParameters();
        expect(queryParameters.get("a")).toContainExactly("1");
        expect(queryParameters.get("b")).toContainExactly("2");
    }

    private <T> APIRequest<Integer> buildValidRequest() {
        return RequestBuilder.<Integer>get(URI_PATH).forResource(Integer.class).forPrivateAPI(1).build();
    }

    private <T> APIRequest<Integer> buildValidRequestFromFullUri() {
        return RequestBuilder.<Integer>get(FULL_URI).forResource(Integer.class).forPrivateAPI(1).build();
    }
}
