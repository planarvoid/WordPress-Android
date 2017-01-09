package com.soundcloud.android.payments.error;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.LogoutActivity;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import org.jetbrains.annotations.NotNull;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

public class WrongUserDialog extends UnrecoverableErrorDialog {

    private final DialogInterface.OnClickListener listener = (dialogInterface, which) -> {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            LogoutActivity.start(getActivity());
        }
        dismiss();
    };

    public static void show(FragmentManager fragmentManager) {
        new WrongUserDialog().show(fragmentManager, PaymentError.DIALOG_TAG);
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = new CustomFontViewBuilder(getActivity())
                .setContent(R.drawable.dialog_payment_error,
                            R.string.payments_error_title_wrong_user,
                            R.string.payments_error_wrong_user).get();

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.payments_sign_out, listener)
                .setNegativeButton(android.R.string.cancel, listener)
                .create();
    }
}
