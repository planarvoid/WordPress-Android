package com.soundcloud.android.configuration;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;

public class ForceUpdateDialog extends DialogFragment {

    private static final String DIALOG_TAG = "force_update_dlg";
    private static final String PLAY_STORE_URL = "market://details?id=com.soundcloud.android";
    private static final String PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=com.soundcloud.android";

    public static void show(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(DIALOG_TAG) == null) {
            final ForceUpdateDialog dialog = new ForceUpdateDialog();
            dialog.setCancelable(false);
            dialog.show(fragmentManager, DIALOG_TAG);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(new CustomFontViewBuilder(getActivity()).setTitle(R.string.kill_switch_message).get())
                .setPositiveButton(R.string.kill_switch_confirm, (d, which) -> closeActivityAndLaunchPlayStore())
                .create();

        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                return onBackPressed();
            }
            return false;
        });

        return dialog;
    }

    private boolean onBackPressed() {
        getActivity().finish();
        return true;
    }

    private void closeActivityAndLaunchPlayStore() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL));
            getActivity().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_WEB_URL));
            getActivity().startActivity(intent);
        }

        getActivity().finish();
    }
}
