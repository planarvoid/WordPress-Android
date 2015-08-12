package com.soundcloud.android.onboarding;

import static com.soundcloud.android.onboarding.OnboardingOperations.EmailOptIn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiObjectContentRequest;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;

public class OnboardingOperationsTest extends AndroidUnitTest {

    private OnboardingOperations operations;

    @Mock private ApiClientRx apiClientRx;

    @Before
    public void setUp() throws Exception {
        operations = new OnboardingOperations(apiClientRx, Schedulers.immediate());
    }

    @Test
    public void shouldMakeAPutRequestOnEmailOptIn() {
        when(apiClientRx.response(any(ApiRequest.class))).thenReturn(Observable.<ApiResponse>empty());

        operations.sendEmailOptIn();

        ArgumentCaptor<ApiRequest> argumentCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).response(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getMethod()).isEqualTo("PUT");
    }

    @Test
    public void shouldAddParametersOnEmailOptIn() throws Exception {
        when(apiClientRx.response(any(ApiRequest.class))).thenReturn(Observable.<ApiResponse>empty());

        operations.sendEmailOptIn();

        ArgumentCaptor<ApiRequest> argumentCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).response(argumentCaptor.capture());
        EmailOptIn content = (EmailOptIn) ((ApiObjectContentRequest) argumentCaptor.getValue()).getContent();
        assertThat(content.newsletter).isTrue();
        assertThat(content.productUpdates).isTrue();
        assertThat(content.surveys).isTrue();
    }

}