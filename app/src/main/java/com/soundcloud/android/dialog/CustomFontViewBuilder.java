package com.soundcloud.android.dialog;

import com.soundcloud.android.R;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomFontViewBuilder {

    private final Context context;
    private final View view;

    public CustomFontViewBuilder(Context context) {
        this.context = context;
        this.view = View.inflate(context, R.layout.dialog_custom_message, null);
    }

    public CustomFontViewBuilder setContent(@DrawableRes int drawable, @StringRes int title, @StringRes int body) {
        setIcon(drawable);
        setTitle(title);
        setMessage(body);
        return this;
    }

    public CustomFontViewBuilder setIcon(@DrawableRes int drawable) {
        final ImageView image = (ImageView) view.findViewById(R.id.custom_dialog_image);
        image.setImageResource(drawable);
        image.setVisibility(View.VISIBLE);
        return this;
    }

    public CustomFontViewBuilder setTitle(@StringRes int title) {
        setTitle(context.getString(title));
        return this;
    }

    public CustomFontViewBuilder setTitle(String title) {
        final TextView titleView = (TextView) view.findViewById(R.id.custom_dialog_title);
        titleView.setText(title);
        return this;
    }

    public CustomFontViewBuilder setMessage(@StringRes int message) {
        setMessage(context.getString(message));
        return this;
    }

    public CustomFontViewBuilder setMessage(String text) {
        final TextView contentView = (TextView) view.findViewById(R.id.custom_dialog_body);
        contentView.setText(text);
        contentView.setVisibility(View.VISIBLE);
        return this;
    }

    public View get() {
        return view;
    }
}
