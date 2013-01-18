package com.soundcloud.android.view.adapter;

import static com.soundcloud.android.utils.ScTextUtils.getTimeElapsed;

import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;

public class TrackRepostActivityRow extends ActivityRow {

    public TrackRepostActivityRow(Context context) {
        super(context);
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

    @Override
    public CharSequence getContentDescription() {
        StringBuilder builder = new StringBuilder();
        builder.append(mActivity.getUser().getDisplayName());
        builder.append(" ");
        builder.append(getContext().getResources().getString(R.string.accessibility_infix_reposted));
        builder.append(" ");
        builder.append(mActivity.getTrack().title);
        builder.append(". ");
        builder.append(getTimeElapsed(getContext().getResources(), mActivity.created_at.getTime(), true));

        return builder.toString();
    }
}
