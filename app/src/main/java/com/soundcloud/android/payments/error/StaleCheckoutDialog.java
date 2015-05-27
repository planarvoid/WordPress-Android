package com.soundcloud.android.payments.error;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.ImageAlertDialog;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

public class StaleCheckoutDialog extends UnrecoverableErrorDialog {

    private final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                openSupport();
            }
            dismiss();
        }
    };

    private void openSupport() {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse(getString(R.string.url_contact_support))));
        }
    }

    public static void show(FragmentManager fragmentManager) {
        new StaleCheckoutDialog().show(fragmentManager, PaymentError.DIALOG_TAG);
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new ImageAlertDialog(getActivity())
                .setContent(R.drawable.dialog_payment_error,
                        R.string.payments_error_title_stale,
                        R.string.payments_error_stale)
                .setPositiveButton(R.string.payments_error_contact_support, listener)
                .setNegativeButton(android.R.string.cancel, listener)
                .create();
    }
}
