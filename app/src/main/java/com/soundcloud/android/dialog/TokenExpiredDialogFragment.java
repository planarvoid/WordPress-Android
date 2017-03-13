package com.soundcloud.android.dialog;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.LogoutActivity;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;

public class TokenExpiredDialogFragment extends DialogFragment {
    public static final String TAG = "TokenExpiredDialog";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = new CustomFontViewBuilder(getActivity())
                .setTitle(R.string.error_unauthorized_title)
                .setMessage(R.string.error_unauthorized_message)
                .get();

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.pref_revoke_access, (dialog, which) -> {
                    LogoutActivity.start(getActivity());
                    dismiss();
                }
                ).create();
    }
}
