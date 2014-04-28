package com.soundcloud.android.activities;

import static com.soundcloud.android.utils.ScTextUtils.getTimeElapsed;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.ActivityRow;
import com.soundcloud.android.image.ImageOperations;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

public class AffiliationActivityRow extends ActivityRow {

    public AffiliationActivityRow(Context context, ImageOperations imageOperations) {
        super(context, imageOperations);
    }

    @Override
    protected Drawable doGetDrawable() {
        Drawable drawable =
                getResources().getDrawable(R.drawable.activity_following);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }

    @Override
    protected boolean fillParcelable(Parcelable p) {
        return activity.getUser() != null;
    }

    @Override
    protected SpannableStringBuilder createSpan() {
        spanBuilder = new SpannableStringBuilder();
        spanBuilder.append("  ").append(activity.getUser().username);
        spanBuilder.setSpan(new StyleSpan(Typeface.BOLD), 1, spanBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanBuilder.append(" " + getContext().getResources().getString(R.string.started_following_you));
        return spanBuilder;
    }

    @Override
    public CharSequence getContentDescription() {
        StringBuilder builder = new StringBuilder();
        builder.append(activity.getUser().getDisplayName());
        builder.append(getContext().getResources().getString(R.string.started_following_you));
        builder.append(". ");
        builder.append(getTimeElapsed(getContext().getResources(), activity.getCreatedAt().getTime(), true));

        return builder.toString();
    }
}
