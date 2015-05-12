package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.payments.error.AlreadySubscribedDialog;
import com.soundcloud.android.payments.error.StaleCheckoutDialog;
import com.soundcloud.android.payments.error.WrongUserDialog;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import javax.inject.Inject;

class PaymentErrorPresenter {

    private final Context context;
    private FragmentManager fragmentManager;

    @Inject
    PaymentErrorPresenter(Context context) {
        this.context = context;
    }

    public void setActivity(FragmentActivity activity) {
        fragmentManager = activity.getSupportFragmentManager();
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
        showText(R.string.payments_connection_error);
    }

    public void showUnknownError() {
        showText(R.string.payments_unknown_error);
    }

    private void showText(int messageId) {
        Toast.makeText(context, messageId, Toast.LENGTH_SHORT).show();
    }

}
