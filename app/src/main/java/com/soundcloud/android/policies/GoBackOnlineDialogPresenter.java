package com.soundcloud.android.policies;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.ImageAlertDialog;

import android.content.res.Resources;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

class GoBackOnlineDialogPresenter {

    private final Resources resources;
    private AppCompatActivity activity;

    @Inject
    public GoBackOnlineDialogPresenter(Resources resources) {
        this.resources = resources;
    }

    public void show(long lastOnlineStatusDate) {
        final int daysToGoOnline = getRemainingDaysToGoOnline(lastOnlineStatusDate);
        new ImageAlertDialog(activity)
                .setContent(R.drawable.dialog_go_online_days,
                        getTitleText(daysToGoOnline),
                        getContentText(daysToGoOnline))
                .setPositiveButton(R.string.offline_dialog_go_online_continue, null)
                .create()
                .show();
    }

    void bindActivity(AppCompatActivity activity) {
        this.activity = activity;
    }

    private String getTitleText(int remainingDays) {
        if (remainingDays == 0) {
            return resources.getString(R.string.offline_dialog_go_online_error_title);
        }
        return resources.getQuantityString(R.plurals.offline_dialog_go_online_warning_title, remainingDays, remainingDays);
    }

    private String getContentText(int remainingDays) {
        if (remainingDays == 0) {
            return resources.getString(R.string.offline_dialog_go_online_error_content);
        }
        return resources.getQuantityString(R.plurals.offline_dialog_go_online_warning_content, remainingDays, remainingDays);
    }

    @VisibleForTesting
    int getRemainingDaysToGoOnline(long lastUpdateTime) {
        final long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastUpdateTime);
        return (int) Math.max(PolicyUpdateController.OFFLINE_DAYS_ERROR_THRESHOLD - days, 0);
    }

}
