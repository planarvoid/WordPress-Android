package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ApiResponseTest {

    private ApiResponse response;
    private ApiRequest request;

    @Before
    public void setUp() {
        request = ApiRequest.Builder.get("/").forPrivateApi(1).build();
        response = new ApiResponse(request, 200, "response");
    }

    @Test
    public void shouldSignalSuccessFor200Response() {
        expect(response.isSuccess()).toEqual(true);
        expect(response.getFailure()).toBeNull();
    }

    @Test
    public void shouldSignalIfResponseHasBody() {
        expect(response.hasResponseBody()).toBeTrue();
    }

    @Test
    public void shouldSignalIfResponseDoesNotHaveABody() {
        expect(new ApiResponse(request, 429, "  ").hasResponseBody()).toBeFalse();
    }

    @Test
    public void shouldReturnSpecifiedResponseBody() {
        expect(response.getResponseBody()).toEqual("response");
    }

    @Test
    public void shouldFailWithActionNotAllowedOn403() {
        final ApiResponse response = new ApiResponse(request, 403, "response");
        expect(response.isNotSuccess()).toBeTrue();
        expect(response.getFailure()).toBeInstanceOf(ApiRequestException.class);
        expect(response.getFailure().reason()).toBe(ApiRequestException.Reason.NOT_ALLOWED);
    }

    @Test
    public void shouldFailWithResourceNotFoundOn404() {
        final ApiResponse response = new ApiResponse(request, 404, "response");
        expect(response.isNotSuccess()).toBeTrue();
        expect(response.getFailure()).toBeInstanceOf(ApiRequestException.class);
        expect(response.getFailure().reason()).toBe(ApiRequestException.Reason.NOT_FOUND);
    }

    @Test
    public void shouldFailWithRateLimitExceededOn429() {
        final ApiResponse response = new ApiResponse(request, 429, "response");
        expect(response.isNotSuccess()).toBeTrue();
        expect(response.getFailure()).toBeInstanceOf(ApiRequestException.class);
        expect(response.getFailure().reason()).toBe(ApiRequestException.Reason.RATE_LIMITED);
    }

    @Test
    public void shouldFailWithUnexpectedResponseForOtherErrorCodes() {
        final ApiResponse response = new ApiResponse(request, 500, "response");
        expect(response.isNotSuccess()).toBeTrue();
        expect(response.getFailure()).toBeInstanceOf(ApiRequestException.class);
        expect(response.getFailure().reason()).toBe(ApiRequestException.Reason.UNEXPECTED_RESPONSE);
    }

    @Test
    public void shouldFailWithUnexpectedResponseForPre2xxCodes() {
        final ApiResponse response = new ApiResponse(request, 199, "response");
        expect(response.isNotSuccess()).toBeTrue();
        expect(response.getFailure()).toBeInstanceOf(ApiRequestException.class);
        expect(response.getFailure().reason()).toBe(ApiRequestException.Reason.UNEXPECTED_RESPONSE);
    }
}
