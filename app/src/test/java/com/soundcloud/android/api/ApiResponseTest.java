package com.soundcloud.android.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

public class ApiResponseTest extends AndroidUnitTest {

    private ApiResponse response;
    private ApiRequest request;

    @Before
    public void setUp() {
        request = ApiRequest.get("/").forPrivateApi().build();
        response = new ApiResponse(request, 200, "response");
    }

    @Test
    public void shouldSignalSuccessFor200Response() {
        assertThat(response.isSuccess()).isEqualTo(true);
        assertThat(response.getFailure()).isNull();
    }

    @Test
    public void shouldSignalIfResponseHasBody() {
        assertThat(response.hasResponseBody()).isTrue();
    }

    @Test
    public void shouldSignalIfResponseDoesNotHaveABody() {
        assertThat(new ApiResponse(request, 429, "  ").hasResponseBody()).isFalse();
    }

    @Test
    public void shouldReturnSpecifiedResponseBody() {
        assertThat(response.getResponseBody()).isEqualTo("response");
    }

    @Test
    public void shouldFailWithActionNotAllowedOn403() {
        final ApiResponse response = new ApiResponse(request, 403, "response");
        assertThat(response.isNotSuccess()).isTrue();
        assertThat(response.getFailure()).isInstanceOf(ApiRequestException.class);
        assertThat(response.getFailure().reason()).isEqualTo(ApiRequestException.Reason.NOT_ALLOWED);
    }

    @Test
    public void shouldFailWithResourceNotFoundOn404() {
        final ApiResponse response = new ApiResponse(request, 404, "response");
        assertThat(response.isNotSuccess()).isTrue();
        assertThat(response.getFailure()).isInstanceOf(ApiRequestException.class);
        assertThat(response.getFailure().reason()).isEqualTo(ApiRequestException.Reason.NOT_FOUND);
    }

    @Test
    public void shouldFailWithValidationErrorOn422() {
        final ApiResponse response = new ApiResponse(request, 422, "response");
        assertThat(response.isNotSuccess()).isTrue();
        assertThat(response.getFailure()).isInstanceOf(ApiRequestException.class);
        assertThat(response.getFailure().reason()).isEqualTo(ApiRequestException.Reason.VALIDATION_ERROR);
    }

    @Test
    public void shouldFailWithRateLimitExceededOn429() {
        final ApiResponse response = new ApiResponse(request, 429, "response");
        assertThat(response.isNotSuccess()).isTrue();
        assertThat(response.getFailure()).isInstanceOf(ApiRequestException.class);
        assertThat(response.getFailure().reason()).isEqualTo(ApiRequestException.Reason.RATE_LIMITED);
    }

    @Test
    public void shouldFailWithUnexpectedResponseForOther4xxCodes() {
        final ApiResponse response = new ApiResponse(request, 433, "response");
        assertThat(response.isNotSuccess()).isTrue();
        assertThat(response.getFailure()).isInstanceOf(ApiRequestException.class);
        assertThat(response.getFailure().reason()).isEqualTo(ApiRequestException.Reason.UNEXPECTED_RESPONSE);
    }

    @Test
    public void shouldFailWithServerErrorForOther5XXCodes() {
        final ApiResponse response = new ApiResponse(request, 501, "response");
        assertThat(response.isNotSuccess()).isTrue();
        assertThat(response.getFailure()).isInstanceOf(ApiRequestException.class);
        assertThat(response.getFailure().reason()).isEqualTo(ApiRequestException.Reason.SERVER_ERROR);
    }

    @Test
    public void shouldFailWithUnexpectedResponseForPre2xxCodes() {
        final ApiResponse response = new ApiResponse(request, 199, "response");
        assertThat(response.isNotSuccess()).isTrue();
        assertThat(response.getFailure()).isInstanceOf(ApiRequestException.class);
        assertThat(response.getFailure().reason()).isEqualTo(ApiRequestException.Reason.UNEXPECTED_RESPONSE);
    }

    @Test
    public void shouldFailWithBadRequestOn400() {
        final ApiResponse response = new ApiResponse(request, 400, "response");
        assertThat(response.isNotSuccess()).isTrue();
        assertThat(response.getFailure()).isInstanceOf(ApiRequestException.class);
        assertThat(response.getFailure().reason()).isEqualTo(ApiRequestException.Reason.BAD_REQUEST);
    }

    @Test
    public void shouldExtractErrorKeyFromBadRequestBody() {
        final ApiResponse response = new ApiResponse(request, 400, "{\"error_key\":\"my_error\"}");
        assertThat(response.getFailure().errorKey()).isEqualTo("my_error");
    }

    @Test
    public void shouldReturnDefaultIfErrorKeyCouldNotBeExtractedFromBadRequestBody() {
        final ApiResponse response = new ApiResponse(request, 400, "not even json");
        assertThat(response.getFailure().errorKey()).isEqualTo(ApiRequestException.ERROR_KEY_NONE);
    }

    // this is only done temporary until we move this to api-mobile, where we will use keys on the server side
    @Test
    public void shouldWritePublicApiValidationErrorToErrorKey() {
        final ApiResponse response = new ApiResponse(request, 422, "{\"errors\":[{\"error_message\":\"Username cannot contain 'soundcloud'\"}]}");
        assertThat(response.getFailure().errorKey()).isEqualTo("Username cannot contain 'soundcloud'");
    }
}
