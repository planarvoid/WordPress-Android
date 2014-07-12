package com.soundcloud.android.activities;

import static com.soundcloud.android.utils.ScTextUtils.getTimeElapsed;

import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.ActivityRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.api.legacy.model.Comment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.api.legacy.model.activities.CommentActivity;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import java.util.Date;

public class CommentActivityRow extends ActivityRow {
    private Comment comment;

    public CommentActivityRow(Context context, ImageOperations imageOperations) {
        super(context, imageOperations);
    }

    @Override
    protected PublicApiUser getOriginUser() {
        return comment.user;
    }

    @Override
    protected Date getOriginCreatedAt() {
        return comment.getCreatedAt();
    }

    @Override
    protected Drawable doGetDrawable() {
        Drawable drawable = getResources().getDrawable(R.drawable.activity_comment);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }

    @Override
    public Urn getResourceUrn() {
        if (comment != null && comment.getUser() != null) {
            return comment.getUser().getUrn();
        }
        return null;
    }

    @Override
    protected boolean fillParcelable(Parcelable p) {
        comment = ((CommentActivity) activity).comment;
        return comment != null;
    }

    @Override
    protected void addSpan(SpannableStringBuilder builder) {
        builder.append(": ");
        builder.setSpan(new StyleSpan(Typeface.BOLD), 1, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append("\"").append(comment.body).append("\"");
    }

    @Override
    public CharSequence getContentDescription() {
        StringBuilder builder = new StringBuilder();
        builder.append(activity.getUser().getDisplayName());
        builder.append(" ");
        builder.append(getContext().getResources().getString(R.string.accessibility_infix_commented));
        builder.append(" ");
        builder.append(activity.getPlayable().title);
        builder.append(": ");
        builder.append(comment.body);
        builder.append(". ");
        builder.append(getTimeElapsed(getContext().getResources(), activity.getCreatedAt().getTime(), true));

        return builder.toString();
    }
}
