package com.soundcloud.android.utils;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;

import android.annotation.TargetApi;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public final class StatusBarUtils {

    @TargetApi(JELLY_BEAN_MR2)
    public static void setFullscreenMode(Activity activity) {
        if (SDK_INT >= JELLY_BEAN_MR2) {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                                                              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @TargetApi(LOLLIPOP)
    public static void setStatusBarColor(Activity activity, int color) {
        if (shouldColorStatusBar()) {
            final Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
        }

    }

    private static boolean shouldColorStatusBar() {
        // Status bar color cannot be changed before Lollipop and we use `windowLightStatusBar` from Marshmallow
        return SDK_INT >= LOLLIPOP;
    }

    public static boolean shouldColorStatusBarIcons() {
        return SDK_INT >= M;
    }

    @TargetApi(M)
    public static void setLightStatusBar(@NonNull View view) {
        if (shouldColorStatusBarIcons()) {
            int flags = view.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            view.setSystemUiVisibility(flags);
        }
    }

    @TargetApi(M)
    public static void clearLightStatusBar(@NonNull View view) {
        if (shouldColorStatusBarIcons()) {
            int flags = view.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            view.setSystemUiVisibility(flags);
        }
    }

    private StatusBarUtils() {
    }
}
