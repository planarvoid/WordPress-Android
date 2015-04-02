package com.soundcloud.android.dialog;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.LogoutActivity;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class TokenExpiredDialogFragment extends DialogFragment {
    public static final String TAG = "TokenExpiredDialog";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialogWrapper.Builder(getActivity()).setTitle(R.string.error_unauthorized_title)
                .setMessage(R.string.error_unauthorized_message).setPositiveButton(
                        R.string.pref_revoke_access, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                LogoutActivity.start(getActivity());
                                dismiss();
                            }
                        }
                ).create();
    }
}
