package com.soundcloud.android.payments;

import com.soundcloud.android.api.ApiRequestException;

import android.support.v4.app.FragmentActivity;

import javax.inject.Inject;

class PaymentErrorController {

    private static final String ERROR_KEY_ALREADY_SUBSCRIBED = "already_subscribed";
    private static final String ERROR_KEY_WRONG_USER = "wrong_user";

    private final PaymentErrorPresenter errorPresenter;

    @Inject
    public PaymentErrorController(PaymentErrorPresenter errorPresenter) {
        this.errorPresenter = errorPresenter;
    }

    public void bind(FragmentActivity activity) {
        errorPresenter.setActivity(activity);
    }

    public void onError(Throwable e) {
        if (e instanceof ApiRequestException) {
            handleApiException((ApiRequestException) e);
        } else {
            errorPresenter.showUnknownError();
        }
    }

    private void handleApiException(ApiRequestException e) {
        switch (e.reason()) {
            case BAD_REQUEST:
                handleBadRequest(e);
                break;
            case NOT_FOUND:
                errorPresenter.showStaleCheckout();
                break;
            default:
                errorPresenter.showConnectionError();
        }
    }

    private void handleBadRequest(ApiRequestException e) {
        switch (e.errorKey()) {
            case ERROR_KEY_ALREADY_SUBSCRIBED:
                errorPresenter.showAlreadySubscribed();
                break;
            case ERROR_KEY_WRONG_USER:
                errorPresenter.showWrongUser();
                break;
            default:
                errorPresenter.showConnectionError();
        }
    }

}
