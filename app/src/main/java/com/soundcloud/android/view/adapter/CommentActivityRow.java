package com.soundcloud.android.view.adapter;

import static com.soundcloud.android.utils.ScTextUtils.getTimeElapsed;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.CommentActivity;
import com.soundcloud.android.utils.images.ImageSize;

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

    public CommentActivityRow(Context context) {
        super(context);
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
    protected Drawable doGetDrawable(boolean pressed) {
        Drawable drawable = getResources().getDrawable(pressed ? R.drawable.activity_comment_white_50 : R.drawable.activity_comment);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }

    @Override
    public String getIconRemoteUri() {
        if (mComment == null || mComment.getUser() == null || mComment.getUser().avatar_url == null) return "";
        return ImageSize.formatUriForList(getContext(), mComment.getUser().avatar_url);
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
