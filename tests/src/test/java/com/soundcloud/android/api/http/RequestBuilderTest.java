package com.soundcloud.android.api.http;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.api.http.SoundCloudAPIRequest.RequestBuilder;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class RequestBuilderTest {
    private static final String URI_PATH = "somepath";


    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBuildRequestIfURIIsNull() throws Exception {
          RequestBuilder.<Integer>get(null).forVersion(1).forResource(Integer.class).forPrivateAPI().build();
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
    public void shouldReturnRequestInstanceWithGetMethodSet() {
        APIRequest<Integer> request = buildValidRequest();
        expect(request.getMethod()).toEqual("GET");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIllegalArgumentExceptionOnNegativeVersionValue() {
        RequestBuilder.<Integer>get(URI_PATH).forVersion(-1).forResource(Integer.class).forPrivateAPI().build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIllegalArgumentExceptionOnZeroVersionValueForPrivateAPI() {
        RequestBuilder.<Integer>get(URI_PATH).forVersion(0).forResource(Integer.class).forPrivateAPI().build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIllegalArgumentExceptionIfNoVersionSetForPrivateAPI() {
        RequestBuilder.<Integer>get(URI_PATH).forResource(Integer.class).forPrivateAPI().build();
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
        expect(request.getResourceType()).toEqual(new TypeToken<Integer>(){});
    }

    @Test
    public void shouldReturnSpecifiedResourceTypeTokenForValidRequest(){
        APIRequest<List<Integer>> request = RequestBuilder.<List<Integer>>get(URI_PATH).forVersion(1).
                forResource(new TypeToken<List<Integer>>() {}).forPrivateAPI().build();
        expect(request.getResourceType()).toEqual(new TypeToken<List<Integer>>(){});
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfAPIModeNotSet(){
        RequestBuilder.<Integer>get(URI_PATH).forVersion(1).
                forResource(Integer.class).build();
    }

    @Test
    public void shouldReturnSpecifiedPrivateAPITarget(){
        APIRequest<Integer> request = buildValidRequest();
        expect(request.isPrivate()).toBeTrue();
    }

    @Test
    public void shouldReturnSpecifiedPublicAPITarget(){
        APIRequest<Integer> request = RequestBuilder.<Integer>get(URI_PATH).forVersion(1).forResource(Integer.class).forPublicAPI().build();
        expect(request.isPrivate()).toBeFalse();
    }

    private <T> APIRequest<Integer> buildValidRequest() {
        return RequestBuilder.<Integer>get(URI_PATH).forVersion(1).forResource(Integer.class).forPrivateAPI().build();
    }
}
