package com.soundcloud.android.payments;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PaymentFailureEvent;
import com.soundcloud.rx.eventbus.EventBusV2;

import android.support.v4.app.FragmentActivity;

import javax.inject.Inject;

class PaymentErrorPresenter {

    private static final String ERROR_KEY_ALREADY_SUBSCRIBED = "already_subscribed";
    private static final String ERROR_KEY_WRONG_USER = "wrong_user";
    private static final String ERROR_KEY_UNCONFIRMED_EMAIL = "unconfirmed_email";

    private final PaymentErrorView errorView;
    private final EventBusV2 eventBus;

    @Inject
    PaymentErrorPresenter(PaymentErrorView errorView, EventBusV2 eventBus) {
        this.errorView = errorView;
        this.eventBus = eventBus;
    }

    public void setActivity(FragmentActivity activity) {
        errorView.bind(activity);
    }

    public void onError(Throwable e) {
        if (e instanceof ApiRequestException) {
            handleApiException((ApiRequestException) e);
        } else {
            errorView.showUnknownError();
            trackFailure("Unknown");
        }
    }

    private void handleApiException(ApiRequestException e) {
        switch (e.reason()) {
            case BAD_REQUEST:
                handleBadRequest(e);
                break;
            case NOT_FOUND:
                errorView.showStaleCheckout();
                trackFailure("Stale checkout");
                break;
            default:
                showConnectionError();
        }
    }

    private void handleBadRequest(ApiRequestException e) {
        switch (e.errorKey()) {
            case ERROR_KEY_ALREADY_SUBSCRIBED:
                errorView.showAlreadySubscribed();
                trackFailure("Already subscribed");
                break;
            case ERROR_KEY_WRONG_USER:
                errorView.showWrongUser();
                trackFailure("Wrong user");
                break;
            case ERROR_KEY_UNCONFIRMED_EMAIL:
                errorView.showUnconfirmedEmail();
                trackFailure("Unconfirmed email");
                break;
            default:
                showConnectionError();
        }
    }

    void showCancelled() {
        errorView.showCancelled();
    }

    void showConnectionError() {
        errorView.showConnectionError();
    }

    void showBillingUnavailable() {
        errorView.showBillingUnavailable();
        trackFailure("Billing unavailable");
    }

    void showVerifyFail() {
        errorView.showVerifyFail();
        trackFailure("Verify fail");
    }

    void showVerifyTimeout() {
        errorView.showVerifyTimeout();
        trackFailure("Verify timeout");
    }

    private void trackFailure(String reason) {
        eventBus.publish(EventQueue.TRACKING, PaymentFailureEvent.create(reason));
    }

}
