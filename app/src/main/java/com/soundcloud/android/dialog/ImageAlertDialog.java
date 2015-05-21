package com.soundcloud.android.dialog;

import com.soundcloud.android.R;

import android.app.Activity;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ImageAlertDialog {

    private final Activity activity;

    public ImageAlertDialog(Activity activity) {
        this.activity = activity;
    }

    public AlertDialog.Builder setContent(@DrawableRes int drawable, @StringRes int title, @StringRes int body) {
        return setContent(drawable, activity.getString(title), activity.getString(body));
    }

    public AlertDialog.Builder setContent(@DrawableRes int drawable, String title, String body) {
        final View customView = View.inflate(activity, R.layout.dialog_custom_message, null);

        setImage(customView, drawable);
        setTitle(customView, title);
        setBody(customView, body);

        return new AlertDialog.Builder(activity)
                .setView(customView);
    }

    private void setImage(View view, @DrawableRes int drawable) {
        final ImageView image = (ImageView) view.findViewById(R.id.custom_dialog_image);
        image.setImageResource(drawable);
    }

    private void setTitle(View view, String text) {
        final TextView titleView = (TextView) view.findViewById(R.id.custom_dialog_title);
        titleView.setText(text);
    }

    private void setBody(View view, String text) {
        final TextView contentView = (TextView) view.findViewById(R.id.custom_dialog_body);
        contentView.setText(text);
    }

}
