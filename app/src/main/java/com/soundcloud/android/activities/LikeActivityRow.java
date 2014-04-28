package com.soundcloud.android.activities;

import static com.soundcloud.android.utils.ScTextUtils.getTimeElapsed;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.ActivityRow;
import com.soundcloud.android.image.ImageOperations;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;

public class LikeActivityRow extends ActivityRow {

    public LikeActivityRow(Context activity, ImageOperations imageOperations) {
        super(activity, imageOperations);
    }

    @Override
    protected boolean fillParcelable(Parcelable p) {
        return activity != null;
    }

    @Override
    protected Drawable doGetDrawable() {
        Drawable drawable =
                getResources().getDrawable(R.drawable.activity_like);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }

    @Override
    public CharSequence getContentDescription() {
        StringBuilder builder = new StringBuilder();
        builder.append(activity.getUser().getDisplayName());
        builder.append(" ");
        builder.append(getContext().getResources().getString(R.string.accessibility_infix_liked));
        builder.append(" ");
        builder.append(activity.getPlayable().title);
        builder.append(". ");
        builder.append(getTimeElapsed(getContext().getResources(), activity.getCreatedAt().getTime(), true));

        return builder.toString();
    }
}

