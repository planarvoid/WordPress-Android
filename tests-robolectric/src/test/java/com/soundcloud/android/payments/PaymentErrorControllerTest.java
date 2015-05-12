package com.soundcloud.android.payments;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.FragmentActivity;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class PaymentErrorControllerTest {

    private PaymentErrorController paymentErrorController;

    @Mock private FragmentActivity activity;
    @Mock private PaymentErrorPresenter errorPresenter;

    private ApiRequest apiRequest;

    @Before
    public void setUp() {
        apiRequest = ApiRequest.get("/").forPrivateApi(1).build();
        paymentErrorController = new PaymentErrorController(errorPresenter);
        paymentErrorController.bind(activity);
    }

    @Test
    public void bindSetsActivityOnPresenter() {
        verify(errorPresenter).setActivity(activity);
    }

    @Test
    public void badRequestFromAlreadySubscribedShowsCorrectError() {
        paymentErrorController.onError(ApiRequestException.badRequest(apiRequest, "already_subscribed"));
        verify(errorPresenter).showAlreadySubscribed();
    }

    @Test
    public void notFoundShowsStaleCheckout() {
        paymentErrorController.onError(ApiRequestException.notFound(apiRequest));
        verify(errorPresenter).showStaleCheckout();
    }

    @Test
    public void unrecognisedApiExceptionShowsConnectionError() {
        paymentErrorController.onError(ApiRequestException.networkError(apiRequest, new IOException()));
        verify(errorPresenter).showConnectionError();
    }

    @Test
    public void nonApiExceptionShowsGenericError() {
        paymentErrorController.onError(new IllegalAccessError());
        verify(errorPresenter).showUnknownError();
    }

}