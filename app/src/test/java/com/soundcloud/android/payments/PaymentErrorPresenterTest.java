package com.soundcloud.android.payments;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.app.FragmentActivity;

import java.io.IOException;

public class PaymentErrorPresenterTest extends AndroidUnitTest {

    private PaymentErrorPresenter paymentErrorPresenter;

    @Mock private FragmentActivity activity;
    @Mock private PaymentErrorView errorPresenter;

    private ApiRequest apiRequest;

    @Before
    public void setUp() {
        apiRequest = ApiRequest.get("/").forPrivateApi(1).build();
        paymentErrorPresenter = new PaymentErrorPresenter(errorPresenter);
        paymentErrorPresenter.setActivity(activity);
    }

    @Test
    public void bindSetsActivityOnPresenter() {
        verify(errorPresenter).bind(activity);
    }

    @Test
    public void badRequestFromAlreadySubscribedShowsCorrectError() {
        paymentErrorPresenter.onError(ApiRequestException.badRequest(apiRequest, "already_subscribed"));
        verify(errorPresenter).showAlreadySubscribed();
    }

    @Test
    public void notFoundShowsStaleCheckout() {
        paymentErrorPresenter.onError(ApiRequestException.notFound(apiRequest));
        verify(errorPresenter).showStaleCheckout();
    }

    @Test
    public void unrecognisedApiExceptionShowsConnectionError() {
        paymentErrorPresenter.onError(ApiRequestException.networkError(apiRequest, new IOException()));
        verify(errorPresenter).showConnectionError();
    }

    @Test
    public void nonApiExceptionShowsGenericError() {
        paymentErrorPresenter.onError(new IllegalAccessError());
        verify(errorPresenter).showUnknownError();
    }

}