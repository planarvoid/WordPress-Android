package com.soundcloud.android.payments;

import com.soundcloud.android.R;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

public class ConversionRestrictionsDialog extends AppCompatDialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.conversion_restrictions_dialog_title)
                .setMessage(R.string.conversion_restrictions_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
}
