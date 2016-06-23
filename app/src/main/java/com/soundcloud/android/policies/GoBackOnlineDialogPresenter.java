package com.soundcloud.android.policies;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.CustomFontViewBuilder;

import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AlertDialog;
import android.view.View;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

class GoBackOnlineDialogPresenter {

    private final Resources resources;

    @Inject
    public GoBackOnlineDialogPresenter(Resources resources) {
        this.resources = resources;
    }

    public void show(Activity activity, long lastOnlineStatusDate) {
        final int daysToGoOnline = getRemainingDaysToGoOnline(lastOnlineStatusDate);
        final View view = new CustomFontViewBuilder(activity)
                .setIcon(R.drawable.dialog_go_online_days)
                .setTitle(getTitleText(daysToGoOnline))
                .setMessage(getContentText(daysToGoOnline)).get();

        new AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton(R.string.offline_dialog_go_online_continue, null)
                .create()
                .show();
    }

    private String getTitleText(int remainingDays) {
        if (remainingDays == 0) {
            return resources.getString(R.string.offline_dialog_go_online_error_title);
        }
        return resources.getQuantityString(R.plurals.offline_dialog_go_online_warning_title,
                                           remainingDays,
                                           remainingDays);
    }

    private String getContentText(int remainingDays) {
        if (remainingDays == 0) {
            return resources.getString(R.string.offline_dialog_go_online_error_content);
        }
        return resources.getQuantityString(R.plurals.offline_dialog_go_online_warning_content,
                                           remainingDays,
                                           remainingDays);
    }

    @VisibleForTesting
    int getRemainingDaysToGoOnline(long lastUpdateTime) {
        final long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastUpdateTime);
        return (int) Math.max(PolicyUpdateController.OFFLINE_DAYS_ERROR_THRESHOLD - days, 0);
    }

}
