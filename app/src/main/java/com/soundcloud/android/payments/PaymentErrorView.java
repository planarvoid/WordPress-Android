package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.payments.error.AlreadySubscribedDialog;
import com.soundcloud.android.payments.error.BillingUnavailableDialog;
import com.soundcloud.android.payments.error.ConnectionErrorDialog;
import com.soundcloud.android.payments.error.StaleCheckoutDialog;
import com.soundcloud.android.payments.error.UnconfirmedEmailDialog;
import com.soundcloud.android.payments.error.VerifyIssueDialog;
import com.soundcloud.android.payments.error.WrongUserDialog;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

import javax.inject.Inject;

class PaymentErrorView {

    private Activity activity;
    private FragmentManager fragmentManager;

    @Inject
    PaymentErrorView() {
    }

    public void bind(FragmentActivity activity) {
        this.activity = activity;
        fragmentManager = activity.getSupportFragmentManager();
    }

    void showCancelled() {
        final View view = new CustomFontViewBuilder(activity)
                .setContent(R.drawable.dialog_payment_error,
                            R.string.payments_error_title_canceled,
                            R.string.payments_error_cancelled).get();
        new AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    void showVerifyFail() {
        VerifyIssueDialog.showFail(fragmentManager);
    }

    void showVerifyTimeout() {
        VerifyIssueDialog.showTimeout(fragmentManager);
    }

    void showBillingUnavailable() {
        BillingUnavailableDialog.show(fragmentManager);
    }

    void showAlreadySubscribed() {
        AlreadySubscribedDialog.show(fragmentManager);
    }

    void showStaleCheckout() {
        StaleCheckoutDialog.show(fragmentManager);
    }

    void showWrongUser() {
        WrongUserDialog.show(fragmentManager);
    }

    void showUnconfirmedEmail() {
        UnconfirmedEmailDialog.show(fragmentManager);
    }

    void showConnectionError() {
        showGenericError();
    }

    void showUnknownError() {
        showGenericError();
    }

    private void showGenericError() {
        ConnectionErrorDialog.show(fragmentManager);
    }

}
