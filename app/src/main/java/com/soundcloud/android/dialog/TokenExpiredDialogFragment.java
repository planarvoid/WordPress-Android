package com.soundcloud.android.dialog;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.settings.Settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class TokenExpiredDialogFragment extends DialogFragment {
    public static final String TAG = "TokenExpiredDialog";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.error_unauthorized_title)
                .setMessage(R.string.error_unauthorized_message).setNegativeButton(
                        R.string.side_menu_settings, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(getActivity(), Settings.class));
                    }
                }).setPositiveButton(
                        android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                }).create();
    }
}
