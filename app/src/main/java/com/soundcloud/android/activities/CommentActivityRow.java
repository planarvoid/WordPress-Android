package com.soundcloud.android.activities;

import static com.soundcloud.android.utils.ScTextUtils.getTimeElapsed;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.ActivityRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.activities.CommentActivity;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import java.util.Date;

public class CommentActivityRow extends ActivityRow {
    private Comment mComment;

    public CommentActivityRow(Context context, ImageOperations imageOperations) {
        super(context, imageOperations);
    }

    @Override
    protected User getOriginUser() {
        return mComment.user;
    }

    @Override
    protected Date getOriginCreatedAt() {
        return mComment.created_at;
    }

    @Override
    protected Drawable doGetDrawable() {
        Drawable drawable = getResources().getDrawable(R.drawable.activity_comment);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }

    @Override
    public Urn getResourceUrn() {
        if (mComment != null && mComment.getUser() != null) {
            return mComment.getUser().getUrn();
        }
        return null;
    }

    @Override
    protected boolean fillParcelable(Parcelable p) {
        mComment = ((CommentActivity) mActivity).comment;
        return mComment != null;
    }

    @Override
    protected void addSpan(SpannableStringBuilder builder) {
        builder.append(": ");
        builder.setSpan(new StyleSpan(Typeface.BOLD), 1, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append("\"").append(mComment.body).append("\"");
    }

    @Override
    public CharSequence getContentDescription() {
        StringBuilder builder = new StringBuilder();
        builder.append(mActivity.getUser().getDisplayName());
        builder.append(" ");
        builder.append(getContext().getResources().getString(R.string.accessibility_infix_commented));
        builder.append(" ");
        builder.append(mActivity.getPlayable().title);
        builder.append(": ");
        builder.append(mComment.body);
        builder.append(". ");
        builder.append(getTimeElapsed(getContext().getResources(), mActivity.created_at.getTime(), true));

        return builder.toString();
    }
}
