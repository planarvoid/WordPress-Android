package com.soundcloud.android.payments.error;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import org.jetbrains.annotations.NotNull;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

public class VerifyIssueDialog extends UnrecoverableErrorDialog {

    private static final String TITLE_ID = "title_id";

    private final DialogInterface.OnClickListener listener = (dialogInterface, which) -> dismiss();

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
        final View view = new CustomFontViewBuilder(getActivity())
                .setContent(R.drawable.dialog_payment_error,
                            getArguments().getInt(TITLE_ID),
                            R.string.payments_error_verification_issue).get();

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.ok, listener)
                .create();
    }

}
