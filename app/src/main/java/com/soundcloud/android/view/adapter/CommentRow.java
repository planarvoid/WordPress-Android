package com.soundcloud.android.view.adapter;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.model.Activity.CommentActivity;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import java.util.Date;

public class CommentRow extends ActivityRow {
    private Comment mComment;

    public CommentRow(Context context, ScBaseAdapter adapter) {
        super(context, adapter);
    }

    @Override
    protected void init() {
        // do nothing
    }

    @Override
    protected Track getTrack() {
        return mComment.track;
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
        Drawable drawable =
                getResources().getDrawable(pressed ? R.drawable.stats_comments_white_50 : R.drawable.stats_comments);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }

    @Override
    public String getIconRemoteUri() {
        if (mComment == null || mComment.getUser() == null || mComment.getUser().avatar_url == null) return "";
        return Consts.GraphicSize.formatUriForList(getContext(), mComment.getUser().avatar_url);
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
}
