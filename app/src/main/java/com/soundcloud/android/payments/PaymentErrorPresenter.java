package com.soundcloud.android.payments;

import com.soundcloud.android.api.ApiRequestException;

import android.support.v4.app.FragmentActivity;

import javax.inject.Inject;

class PaymentErrorPresenter {

    private static final String ERROR_KEY_ALREADY_SUBSCRIBED = "already_subscribed";
    private static final String ERROR_KEY_WRONG_USER = "wrong_user";

    private PaymentErrorView errorView;

    @Inject
    PaymentErrorPresenter(PaymentErrorView errorView) {
        this.errorView = errorView;
    }

    public void setActivity(FragmentActivity activity) {
        errorView.bind(activity);
    }

    public void onError(Throwable e) {
        if (e instanceof ApiRequestException) {
            handleApiException((ApiRequestException) e);
        } else {
            errorView.showUnknownError();
        }
    }

    private void handleApiException(ApiRequestException e) {
        switch (e.reason()) {
            case BAD_REQUEST:
                handleBadRequest(e);
                break;
            case NOT_FOUND:
                errorView.showStaleCheckout();
                break;
            default:
                errorView.showConnectionError();
        }
    }

    private void handleBadRequest(ApiRequestException e) {
        switch (e.errorKey()) {
            case ERROR_KEY_ALREADY_SUBSCRIBED:
                errorView.showAlreadySubscribed();
                break;
            case ERROR_KEY_WRONG_USER:
                errorView.showWrongUser();
                break;
            default:
                errorView.showConnectionError();
        }
    }

    public void showCancelled() {
        errorView.showCancelled();
    }

    public void showBillingUnavailable() {
        errorView.showBillingUnavailable();
    }

    public void showConnectionError() {
        errorView.showConnectionError();
    }

    public void showVerifyFail() {
        errorView.showVerifyFail();
    }

    public void showVerifyTimeout() {
        errorView.showVerifyTimeout();
    }

}
