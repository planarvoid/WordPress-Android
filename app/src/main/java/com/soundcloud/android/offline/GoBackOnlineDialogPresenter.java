package com.soundcloud.android.offline;

import com.soundcloud.android.R;
import com.soundcloud.android.crop.util.VisibleForTesting;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

class GoBackOnlineDialogPresenter {

    @Inject
    public GoBackOnlineDialogPresenter() {
    }

    public void show(Activity activity, long lastOnlineStatusDate) {
        final int remainingDaysToGoOnline = getRemainingDaysToGoOnline(lastOnlineStatusDate);
        final View dialogView = View.inflate(activity, R.layout.dialog_go_back_offline, null);

        setTitle(activity, dialogView, remainingDaysToGoOnline);
        setContent(activity, dialogView, remainingDaysToGoOnline);

        new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setPositiveButton(R.string.offline_dialog_go_online_continue, null)
                .create()
                .show();
    }

    private void setContent(Activity activity, View view, int remainingDays) {
        final TextView contentView = (TextView) view.findViewById(R.id.offline_dialog_go_online_content);
        contentView.setText(getContentText(activity, remainingDays));
    }

    private void setTitle(Activity activity, View view, int remainingDays) {
        final TextView titleView = (TextView) view.findViewById(R.id.offline_dialog_go_online_title);
        titleView.setText(getTitleText(activity, remainingDays));
    }

    private String getTitleText(Activity activity, int remainingDays) {
        if (remainingDays == 0) {
            return activity.getString(R.string.offline_dialog_go_online_error_title);
        }
        return activity.getResources().getQuantityString(R.plurals.offline_dialog_go_online_warning_title, remainingDays, remainingDays);
    }

    private String getContentText(Activity activity, int remainingDays) {
        if (remainingDays == 0) {
            return activity.getString(R.string.offline_dialog_go_online_error_content);
        }
        return activity.getResources().getQuantityString(R.plurals.offline_dialog_go_online_warning_content, remainingDays, remainingDays);
    }

    @VisibleForTesting
    int getRemainingDaysToGoOnline(long lastUpdateTime) {
        final long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastUpdateTime);
        return (int) Math.max(PolicyUpdateController.OFFLINE_DAYS_ERROR_THRESHOLD - days, 0);
    }
}
