package com.soundcloud.android.view.adapter;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ScBaseAdapter;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;

public class TrackRepostActivityRow extends ActivityRow {

    public TrackRepostActivityRow(Context context, ScBaseAdapter adapter) {
        super(context, adapter);
    }

    @Override
    protected Drawable doGetDrawable(boolean pressed) {
        Drawable drawable = getResources().getDrawable(pressed ? R.drawable.activity_repost_white_50 : R.drawable.activity_repost);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }

    @Override
    protected boolean fillParcelable(Parcelable p) {
        return mActivity.getUser() != null;
    }
}
