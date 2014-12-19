package com.soundcloud.android.dialog;

import android.app.Activity;
import android.view.WindowManager;

public class DialogHelper {

    @Deprecated // Use DialogFragment instead.
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
