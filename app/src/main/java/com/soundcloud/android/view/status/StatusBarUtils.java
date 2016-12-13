package com.soundcloud.android.view.status;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public final class StatusBarUtils {

    @TargetApi(LOLLIPOP)
    static int getStatusBarColor(AppCompatActivity activity) {
        if (shouldColorStatusBar()) {
            return activity.getWindow().getStatusBarColor();
        }
        return 0;
    }

    @TargetApi(LOLLIPOP)
    static void setStatusBarColor(Activity activity, int color) {
        if (shouldColorStatusBar()) {
            final Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
        }
    }

    public static int getStatusBarHeight(Activity activity){
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return activity.getResources().getDimensionPixelSize(resourceId);
        }

        Rect rectangle = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);
        return rectangle.top;
    }

    private static boolean shouldColorStatusBar() {
        // Status bar color cannot be changed before Lollipop and we use `windowLightStatusBar` from Marshmallow
        return SDK_INT >= LOLLIPOP;
    }

    private static boolean shouldColorStatusBarIcons() {
        return SDK_INT >= M;
    }

    @TargetApi(M)
    static void setLightStatusBar(@NonNull View view) {
        if (shouldColorStatusBarIcons()) {
            View rootView = view.getRootView();
            int flags = rootView.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            rootView.setSystemUiVisibility(flags);
        }
    }

    static boolean isLightStatusBar(View view) {
        if (shouldColorStatusBarIcons()) {
            int flags = view.getRootView().getSystemUiVisibility();
            return (flags & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) != 0;
        }
        return false;
    }

    @TargetApi(M)
    static void clearLightStatusBar(@NonNull View view) {
        if (shouldColorStatusBarIcons()) {
            View rootView = view.getRootView();
            int flags = rootView.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            rootView.setSystemUiVisibility(flags);
        }
    }

    private StatusBarUtils() {
    }
}
