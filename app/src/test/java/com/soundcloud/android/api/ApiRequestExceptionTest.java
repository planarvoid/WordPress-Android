package com.soundcloud.android.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class ApiRequestExceptionTest extends AndroidUnitTest {

    private ApiRequest request;
    private ApiResponse response;

    @Before
    public void setUp() {
        request = ApiRequest.get("/").forPrivateApi().build();
        response = new ApiResponse(request, 400, "bad request");
    }

    @Test
    public void errorKeyIsSetFromBadRequest () {
        assertThat(ApiRequestException.badRequest(request, response, "some_error").errorKey())
                .isEqualTo("some_error");
    }

    @Test
    public void errorKeyIsNoneIfNotSet() {
        assertThat(ApiRequestException.networkError(request, new IOException()).errorKey())
                .isEqualTo(ApiRequestException.ERROR_KEY_NONE);
    }

}
