package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;

public class ConversionRestrictionsDialog extends AppCompatDialogFragment {

    private static final String TRIAL_DAYS = "trial_days";

    public static ConversionRestrictionsDialog create(int trialDays) {
        final ConversionRestrictionsDialog dialog = new ConversionRestrictionsDialog();
        Bundle args = new Bundle();
        args.putInt(TRIAL_DAYS, trialDays);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = new CustomFontViewBuilder(getActivity())
                .setTitle(R.string.conversion_restrictions_dialog_title)
                .setMessage(formatBody())
                .get();
        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    private String formatBody() {
        int trialDays = getArguments().getInt(TRIAL_DAYS);
        if (trialDays > 0) {
            return getString(R.string.conversion_restrictions_message_trial, trialDays, trialDays, trialDays);
        } else {
            return getString(R.string.conversion_restrictions_message_no_trial);
        }
    }

}
