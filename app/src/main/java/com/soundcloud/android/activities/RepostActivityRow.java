package com.soundcloud.android.activities;

import static com.soundcloud.android.utils.ScTextUtils.getTimeElapsed;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.ActivityRow;
import com.soundcloud.android.image.ImageOperations;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;

public class RepostActivityRow extends ActivityRow {

    public RepostActivityRow(Context context, ImageOperations imageOperations) {
        super(context, imageOperations);
    }

    @Override
    protected Drawable doGetDrawable() {
        Drawable drawable = getResources().getDrawable(R.drawable.activity_repost);
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
        builder.append(mActivity.getPlayable().title);
        builder.append(". ");
        builder.append(getTimeElapsed(getContext().getResources(), mActivity.getCreatedAt().getTime(), true));

        return builder.toString();
    }
}
