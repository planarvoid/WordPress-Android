package com.soundcloud.android.payments.error;

import com.soundcloud.android.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

public class AlreadySubscribedDialog extends DialogFragment {

    private static final String TAG = "AlreadySubscribed";

    public static void show(FragmentManager fragmentManager) {
        new AlreadySubscribedDialog().show(fragmentManager, TAG);
    }

    private DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int which) {
            dismiss();
        }
    };

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.payments_error_title)
                .setMessage(R.string.payments_error_already_subscribed)
                .setPositiveButton(android.R.string.ok, listener)
                .create();
    }

}
