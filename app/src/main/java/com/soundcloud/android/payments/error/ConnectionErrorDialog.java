package com.soundcloud.android.payments.error;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.dialog.LoggingDialogFragment;
import org.jetbrains.annotations.NotNull;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

public class ConnectionErrorDialog extends LoggingDialogFragment {

    private final DialogInterface.OnClickListener listener = (dialogInterface, which) -> dismiss();

    public static void show(FragmentManager fragmentManager) {
        new ConnectionErrorDialog().show(fragmentManager, PaymentError.DIALOG_TAG);
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = new CustomFontViewBuilder(getActivity()).setContent(
                R.drawable.dialog_payment_error,
                R.string.payments_error_title_unavailable,
                R.string.payments_error_unavailable).get();

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.ok, listener)
                .create();
    }
}
