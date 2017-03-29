package com.soundcloud.android.settings;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

public class OfflineStorageErrorDialog extends DialogFragment {

    private static final String TAG = "OfflineStorageErrorDialog";

    public static void show(FragmentManager fragmentManager) {
        new OfflineStorageErrorDialog().show(fragmentManager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = new CustomFontViewBuilder(getActivity())
                .setTitle(R.string.offline_storage_error_dialog_title)
                .setMessage(R.string.offline_storage_error_dialog_message).get();

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.ok_got_it, null)
                .setNegativeButton(R.string.go_to_settings, (dialog, which) -> startActivity(new Intent(getActivity(), ChangeStorageLocationActivity.class)))
                .create();
    }
}
