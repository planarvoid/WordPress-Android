package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;

import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.DrawableRes;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

class GoBackOnlineDialogPresenter {

    private final Resources resources;

    @Inject
    public GoBackOnlineDialogPresenter(Resources resources) {
        this.resources = resources;
    }

    public void show(Activity activity, long lastOnlineStatusDate) {
        final int remainingDaysToGoOnline = getRemainingDaysToGoOnline(lastOnlineStatusDate);
        final View dialogView = View.inflate(activity, R.layout.dialog_custom_message, null);

        setImage(dialogView, R.drawable.offline_dialog_go_online_days);
        setTitle(dialogView, remainingDaysToGoOnline);
        setBody(dialogView, remainingDaysToGoOnline);

        new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setPositiveButton(R.string.offline_dialog_go_online_continue, null)
                .create()
                .show();
    }

    private void setImage(View view, @DrawableRes int drawable) {
        final ImageView image = (ImageView) view.findViewById(R.id.custom_dialog_image);
        image.setImageResource(drawable);
    }

    private void setTitle(View view, int remainingDays) {
        final TextView titleView = (TextView) view.findViewById(R.id.custom_dialog_title);
        titleView.setText(getTitleText(remainingDays));
    }

    private void setBody(View view, int remainingDays) {
        final TextView contentView = (TextView) view.findViewById(R.id.custom_dialog_body);
        contentView.setText(getContentText(remainingDays));
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
