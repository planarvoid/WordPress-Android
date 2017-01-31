package com.soundcloud.android.onboarding.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class RecoverPasswordOperationsTest extends AndroidUnitTest {

    @Mock ApiClient apiClient;
    private final ApiRequest request = ApiRequest.post(ApiEndpoints.RESET_PASSWORD.path())
                                                 .forPrivateApi()
                                                 .withContent("test")
                                                 .build();

    private RecoverPasswordOperations operations;

    @Before
    public void setUp() throws Exception {
        operations = new RecoverPasswordOperations(apiClient);
    }

    @Test
    public void success() throws Exception {
        when(apiClient.fetchResponse(any())).thenReturn(new ApiResponse(request, 200, "cool stuff"));
        operations.recoverPassword("any@email.com");

        verify(apiClient).fetchResponse(any());
    }

    @Test
    public void error() throws Exception {
        when(apiClient.fetchResponse(any())).thenReturn(new ApiResponse(request, 400, "not cool"));
        assertThat(operations.recoverPassword("valid@email.com").isSuccess()).isFalse();
    }

    @Test
    public void unknownEmail() throws Exception {
        when(apiClient.fetchResponse(any())).thenReturn(new ApiResponse(request, 422, "identifier_not_found"));
        assertThat(operations.recoverPassword("unknown@email.com").isSuccess()).isFalse();
        assertThat(operations.recoverPassword("unknown@email.com").getFailure().reason()).isEqualTo(ApiRequestException.Reason.VALIDATION_ERROR);
    }
}
