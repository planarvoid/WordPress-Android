package com.soundcloud.android.accounts;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class MeOperationsTest extends AndroidUnitTest {

    @Mock ApiClientRxV2 apiClientRx;
    private MeOperations meOperations;

    @Before
    public void setUp() throws Exception {
        meOperations = new MeOperations(apiClientRx, Schedulers.trampoline());
    }

    @Test
    public void requestsEmailConfirmationThroughApiMobile() {
        when(apiClientRx.ignoreResultRequest(argThat(isApiRequestTo("POST", "/me/emails/confirmation")))).thenReturn(Completable.complete());

        meOperations.resendEmailConfirmation()
                    .test()
                    .assertComplete();
    }
}
