package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;

public class ConversionRestrictionsDialog extends AppCompatDialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = new CustomFontViewBuilder(getActivity())
                .setTitle(R.string.conversion_restrictions_dialog_title)
                .setMessage(R.string.conversion_restrictions_dialog_message)
                .get();

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
}
