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

    private static final String TAG = "payment_error";

    private final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int which) {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(getString(R.string.url_contact_support))));
            }
            dismiss();
        }
    };

    public static void show(FragmentManager fragmentManager) {
        new StaleCheckoutDialog().show(fragmentManager, TAG);
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new ImageAlertDialog(getActivity())
                .setContent(R.drawable.dialog_payment_error,
                        R.string.payments_error_title,
                        R.string.payments_error_stale_checkout)
                .setPositiveButton(R.string.payments_error_contact_support, listener)
                .create();
    }
}
