package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.ImageAlertDialog;
import com.soundcloud.android.payments.error.AlreadySubscribedDialog;
import com.soundcloud.android.payments.error.BillingUnavailableDialog;
import com.soundcloud.android.payments.error.ConnectionErrorDialog;
import com.soundcloud.android.payments.error.StaleCheckoutDialog;
import com.soundcloud.android.payments.error.VerifyIssueDialog;
import com.soundcloud.android.payments.error.WrongUserDialog;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import javax.inject.Inject;

class PaymentErrorView {

    private Activity activity;
    private FragmentManager fragmentManager;

    @Inject
    public PaymentErrorView() {}

    public void bind(FragmentActivity activity) {
        this.activity = activity;
        fragmentManager = activity.getSupportFragmentManager();
    }

    public void showCancelled() {
        new ImageAlertDialog(activity)
                .setContent(R.drawable.dialog_payment_error,
                        R.string.payments_error_title_canceled,
                        R.string.payments_error_cancelled)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    public void showVerifyFail() {
        VerifyIssueDialog.showFail(fragmentManager);
    }

    public void showVerifyTimeout() {
        VerifyIssueDialog.showTimeout(fragmentManager);
    }

    public void showBillingUnavailable() {
        BillingUnavailableDialog.show(fragmentManager);
    }

    public void showAlreadySubscribed() {
        AlreadySubscribedDialog.show(fragmentManager);
    }

    public void showStaleCheckout() {
        StaleCheckoutDialog.show(fragmentManager);
    }

    public void showWrongUser() {
        WrongUserDialog.show(fragmentManager);
    }

    public void showConnectionError() {
        showGenericError();
    }

    public void showUnknownError() {
        showGenericError();
    }

    private void showGenericError() {
        ConnectionErrorDialog.show(fragmentManager);
    }

}
