package com.soundcloud.android.payments.error;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

public class StaleCheckoutDialog extends UnrecoverableErrorDialog {

    private final DialogInterface.OnClickListener listener = (dialogInterface, which) -> {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            openSupport();
        }
        dismiss();
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
        final View view = new CustomFontViewBuilder(getActivity())
                .setContent(R.drawable.dialog_payment_error,
                            R.string.payments_error_title_stale,
                            R.string.payments_error_stale).get();

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.contact_support, listener)
                .setNegativeButton(android.R.string.cancel, listener)
                .create();
    }
}
