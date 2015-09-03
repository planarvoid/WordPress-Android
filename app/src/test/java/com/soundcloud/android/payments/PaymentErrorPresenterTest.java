package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PaymentFailureEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.app.FragmentActivity;

import java.io.IOException;

public class PaymentErrorPresenterTest extends AndroidUnitTest {

    private PaymentErrorPresenter paymentErrorPresenter;

    @Mock private FragmentActivity activity;
    @Mock private PaymentErrorView errorPresenter;

    private TestEventBus eventBus;

    private ApiRequest apiRequest;
    private ApiResponse apiResponse;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        apiRequest = ApiRequest.get("/").forPrivateApi(1).build();
        apiResponse = new ApiResponse(apiRequest, 200, "body");
        paymentErrorPresenter = new PaymentErrorPresenter(errorPresenter, eventBus);
        paymentErrorPresenter.setActivity(activity);
    }

    @Test
    public void bindSetsActivityOnPresenter() {
        verify(errorPresenter).bind(activity);
    }

    @Test
    public void badRequestFromAlreadySubscribedShowsCorrectError() {
        paymentErrorPresenter.onError(ApiRequestException.badRequest(apiRequest, apiResponse, "already_subscribed"));
        verify(errorPresenter).showAlreadySubscribed();
    }

    @Test
    public void badRequestFromUnconfirmedEmailShowsCorrectError() {
        paymentErrorPresenter.onError(ApiRequestException.badRequest(apiRequest, apiResponse, "unconfirmed_email"));
        verify(errorPresenter).showUnconfirmedEmail();
    }

    @Test
    public void notFoundShowsStaleCheckout() {
        paymentErrorPresenter.onError(ApiRequestException.notFound(apiRequest, apiResponse));
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

    @Test
    public void sendsPaymentFailureEventForKnownFailureCase() {
        paymentErrorPresenter.onError(ApiRequestException.badRequest(apiRequest, apiResponse, "unconfirmed_email"));
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PaymentFailureEvent.class);
    }

    @Test
    public void doesNotSendPaymentFailureEventForConnectionError() {
        paymentErrorPresenter.onError(ApiRequestException.networkError(apiRequest, new IOException()));
        paymentErrorPresenter.showConnectionError();
        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void doesNotSendPaymentFailureEventForCancel() {
        paymentErrorPresenter.showCancelled();
        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

}