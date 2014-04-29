package com.soundcloud.android.onboarding;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.onboarding.OnboardingOperations.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.APIResponse;
import com.soundcloud.android.api.http.RxHttpClient;
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
    public void shouldMakeAPostRequestOnEmailOptIn() {
        when(rxHttpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>empty());

        operations.sendEmailOptIn();

        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(rxHttpClient).fetchResponse(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("POST");
    }

    @Test
    public void shouldAddParametersOnEmailOptIn() throws Exception {
        when(rxHttpClient.fetchResponse(any(APIRequest.class))).thenReturn(Observable.<APIResponse>empty());

        operations.sendEmailOptIn();

        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(rxHttpClient).fetchResponse(argumentCaptor.capture());
        EmailOptIn content = (EmailOptIn) argumentCaptor.getValue().getContent();
        expect(content.newsletter).toBeTrue();
        expect(content.productUpdates).toBeTrue();
        expect(content.surveys).toBeTrue();
    }

}