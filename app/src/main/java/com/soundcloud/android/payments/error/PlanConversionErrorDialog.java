package com.soundcloud.android.payments.error;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;

public class PlanConversionErrorDialog extends AppCompatDialogFragment {

    private static final String PLAN_CONVERSION_ERROR_MESSAGE = "plan_conversion_error_message";

    public static PlanConversionErrorDialog createWithMessage(String message) {
        final PlanConversionErrorDialog dialog = new PlanConversionErrorDialog();
        Bundle args = new Bundle();
        args.putString(PLAN_CONVERSION_ERROR_MESSAGE, message);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = new CustomFontViewBuilder(getActivity())
                .setTitle(R.string.plan_conversion_error_dialog_title)
                .setMessage(getMessage())
                .get();
        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    private String getMessage() {
        String genericMessage = getString(R.string.plan_conversion_error_message_generic);
        return getArguments() == null ? genericMessage :
               getArguments().getString(PLAN_CONVERSION_ERROR_MESSAGE, genericMessage);
    }
}
