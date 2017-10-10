package com.soundcloud.android.upgrade;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.dialog.LoggingDialogFragment;
import com.soundcloud.android.navigation.NavigationExecutor;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

import javax.inject.Inject;

public class UnrecoverableErrorDialog extends LoggingDialogFragment {

    private static final String DIALOG_TAG = "go_onboarding_error_dlg";

    @Inject NavigationExecutor navigationExecutor;
    private final DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            navigationExecutor.openHomeAsRootScreen(getActivity());
        }
    };

    public UnrecoverableErrorDialog() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    public static void show(FragmentManager fragmentManager) {
        new UnrecoverableErrorDialog().show(fragmentManager, DIALOG_TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = new CustomFontViewBuilder(getActivity())
                .setTitle(R.string.go_onboarding_error_dialog_title)
                .setMessage(R.string.go_onboarding_error_dialog_msg)
                .get();

        return new AlertDialog.Builder(getActivity())
                .setCancelable(false)
                .setView(view)
                .setPositiveButton(R.string.go_onboarding_error_dialog_button, clickListener)
                .create();
    }
}
