package com.soundcloud.android.dialog;

import android.app.Activity;
import android.view.WindowManager;

public final class DialogHelper {
    private DialogHelper() {
        // No instantiation
    }

    @Deprecated // Use DialogFragment instead.
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public static void safeShowDialog(Activity activity, int dialogId) {
        if (!activity.isFinishing()) {
            try {
                activity.showDialog(dialogId);
            } catch (WindowManager.BadTokenException ignored) {
                // the !isFinishing() check should prevent these - but not always
            }
        }
    }
}
