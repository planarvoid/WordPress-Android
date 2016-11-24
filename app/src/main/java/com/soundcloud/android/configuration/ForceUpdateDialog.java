package com.soundcloud.android.configuration;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

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

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setView(new CustomFontViewBuilder(getActivity()).setTitle(R.string.kill_switch_message).get())
                .setPositiveButton(R.string.kill_switch_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        closeActivityAndLaunchPlayStore();
                    }
                })
                .create();
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
