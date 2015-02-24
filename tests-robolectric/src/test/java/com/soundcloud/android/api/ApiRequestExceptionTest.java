package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class ApiRequestExceptionTest {

    private ApiRequest request;

    @Before
    public void setUp() {
        request = ApiRequest.Builder.get("/").forPrivateApi(1).build();
    }

    @Test
    public void errorKeyIsSetFromBadRequest () {
        expect(ApiRequestException.badRequest(request, "some_error").errorKey())
                .toBe("some_error");
    }

    @Test
    public void errorKeyIsNoneIfNotSet() {
        expect(ApiRequestException.networkError(request, new IOException()).errorKey())
                .toBe(ApiRequestException.ERROR_KEY_NONE);
    }

}