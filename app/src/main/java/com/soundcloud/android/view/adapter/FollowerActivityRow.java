package com.soundcloud.android.view.adapter;

import com.soundcloud.android.R;
import com.soundcloud.android.model.User;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

public class FollowerActivityRow extends ActivityRow {
    public User user;

    public FollowerActivityRow(Context context) {
        super(context);
    }

    @Override
    protected void init() {
        // do nothing
    }

    @Override
    protected Drawable doGetDrawable(boolean pressed) {
        Drawable drawable =
                getResources().getDrawable(pressed ? R.drawable.activity_following_white_50 : R.drawable.activity_following);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }

    @Override
    protected void addSpan(SpannableStringBuilder builder) {
        builder.append(": ");
        builder.setSpan(new StyleSpan(Typeface.BOLD), 1, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append("\"").append("Started Following You").append("\"");
    }

    @Override
    protected boolean fillParcelable(Parcelable p) {
        return true;
    }
}
