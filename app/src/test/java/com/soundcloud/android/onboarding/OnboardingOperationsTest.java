package com.soundcloud.android.onboarding;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.onboarding.OnboardingOperations.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;

@RunWith(SoundCloudTestRunner.class)
public class OnboardingOperationsTest {

    private OnboardingOperations operations;

    @Mock
    private RxHttpClient rxHttpClient;

    @Before
    public void setUp() throws Exception {
        operations = new OnboardingOperations(rxHttpClient);
    }

    @Test
    public void shouldMakeAPutRequestOnEmailOptIn() {
        when(rxHttpClient.fetchResponse(any(ApiRequest.class))).thenReturn(Observable.<ApiResponse>empty());

        operations.sendEmailOptIn();

        ArgumentCaptor<ApiRequest> argumentCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(rxHttpClient).fetchResponse(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("PUT");
    }

    @Test
    public void shouldAddParametersOnEmailOptIn() throws Exception {
        when(rxHttpClient.fetchResponse(any(ApiRequest.class))).thenReturn(Observable.<ApiResponse>empty());

        operations.sendEmailOptIn();

        ArgumentCaptor<ApiRequest> argumentCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(rxHttpClient).fetchResponse(argumentCaptor.capture());
        EmailOptIn content = (EmailOptIn) argumentCaptor.getValue().getContent();
        expect(content.newsletter).toBeTrue();
        expect(content.productUpdates).toBeTrue();
        expect(content.surveys).toBeTrue();
    }

}