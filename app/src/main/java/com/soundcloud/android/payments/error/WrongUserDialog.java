package com.soundcloud.android.payments.error;

import com.soundcloud.android.R;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

public class WrongUserDialog extends UnrecoverableErrorDialog {

    private static final String TAG = "payment_error";

    private final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int which) {
            dismiss();
        }
    };

    public static void show(FragmentManager fragmentManager) {
        new WrongUserDialog().show(fragmentManager, TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.payments_error_title)
                .setMessage(R.string.payments_error_wrong_user)
                .setPositiveButton(android.R.string.ok, listener)
                .create();
    }
}
