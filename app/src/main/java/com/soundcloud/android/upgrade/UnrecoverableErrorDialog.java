package com.soundcloud.android.upgrade;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dialog.CustomFontViewBuilder;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

import javax.inject.Inject;

public class UnrecoverableErrorDialog extends DialogFragment {

    private static final String DIALOG_TAG = "go_onboarding_error_dlg";

    @Inject Navigator navigator;
    private final DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            navigator.openHomeAsRootScreen(getActivity());
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
