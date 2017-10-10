package com.soundcloud.android.payments.error;

import com.soundcloud.android.dialog.LoggingDialogFragment;

import android.app.Activity;
import android.content.DialogInterface;

abstract class UnrecoverableErrorDialog extends LoggingDialogFragment {

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        finishParent();
    }

    private void finishParent() {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }
}
