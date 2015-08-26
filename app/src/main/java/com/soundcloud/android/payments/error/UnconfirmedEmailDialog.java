package com.soundcloud.android.payments.error;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.ImageAlertDialog;
import org.jetbrains.annotations.NotNull;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

public class UnconfirmedEmailDialog extends UnrecoverableErrorDialog {

    private final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int which) {
            dismiss();
        }
    };

    public static void show(FragmentManager fragmentManager) {
        new UnconfirmedEmailDialog().show(fragmentManager, PaymentError.DIALOG_TAG);
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new ImageAlertDialog(getActivity())
                .setContent(R.drawable.dialog_payment_error,
                        R.string.payments_error_title_unconfirmed_email,
                        R.string.payments_error_unconfirmed_email)
                .setPositiveButton(android.R.string.ok, listener)
                .create();
    }

}
