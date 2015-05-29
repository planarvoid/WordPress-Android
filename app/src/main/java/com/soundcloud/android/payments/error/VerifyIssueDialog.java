package com.soundcloud.android.payments.error;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.ImageAlertDialog;
import org.jetbrains.annotations.NotNull;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentManager;

public class VerifyIssueDialog extends UnrecoverableErrorDialog {

    private static final String TITLE_ID = "title_id";

    private final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int which) {
            dismiss();
        }
    };

    public static void showFail(FragmentManager fragmentManager) {
        show(fragmentManager, R.string.payments_error_title_verification_fail);
    }

    public static void showTimeout(FragmentManager fragmentManager) {
        show(fragmentManager, R.string.payments_error_title_verification_timeout);
    }

    private static void show(FragmentManager fragmentManager, @StringRes int bodyTextId) {
        VerifyIssueDialog dialog = new VerifyIssueDialog();
        Bundle args = new Bundle();
        args.putInt(TITLE_ID, bodyTextId);
        dialog.setArguments(args);
        dialog.show(fragmentManager, PaymentError.DIALOG_TAG);
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new ImageAlertDialog(getActivity())
                .setContent(R.drawable.dialog_payment_error,
                        getArguments().getInt(TITLE_ID),
                        R.string.payments_error_verification_issue)
                .setPositiveButton(android.R.string.ok, listener)
                .create();
    }

}
