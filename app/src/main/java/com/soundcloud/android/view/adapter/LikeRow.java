package com.soundcloud.android.view.adapter;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ScBaseAdapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;

public class LikeRow extends ActivityRow {

    public LikeRow(Context activity, ScBaseAdapter adapter) {
        super(activity, adapter);
    }

    @Override
    protected boolean fillParcelable(Parcelable p) {
        return mActivity != null;
    }

    @Override
    protected Drawable doGetDrawable(boolean pressed) {
        Drawable drawable =
                getResources().getDrawable(pressed ? R.drawable.stats_favorites_white_50 : R.drawable.stats_favorited);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }
}

