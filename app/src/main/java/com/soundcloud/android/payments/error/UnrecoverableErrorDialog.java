package com.soundcloud.android.payments.error;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;

abstract class UnrecoverableErrorDialog extends DialogFragment {

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
