package com.soundcloud.android.view;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.SectionedCommentAdapter;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

import java.util.Date;

public class CommentRow extends ActivityRow {
    private Comment mComment;
    private Track mTrack;

    public CommentRow(ScActivity activity, LazyBaseAdapter adapter) {
        super(activity, adapter);
    }

    public CommentRow(ScActivity mActivity, SectionedCommentAdapter sectionedCommentAdapter, Track track) {
        super(mActivity, sectionedCommentAdapter);
        mTrack = track;
    }

    protected boolean fillParcelable(Parcelable p){
        mComment = (Comment) p;
        return mComment != null;
    }

    protected ActivityType getType() {
        return ActivityType.Comment;
    }

    protected Track getTrack(){
        return mTrack == null ? mComment.track : mTrack;
    }

    protected Comment getComment(){
        return mComment;
    }

    protected User getOriginUser(){
        return mComment.user;
    }

    protected Date getOriginCreatedAt(){
        return mComment.created_at;
    }

    @Override
    public String getIconRemoteUri() {
        if (mComment == null || mComment.getUser() == null || mComment.getUser().avatar_url == null) return "";
        return ImageUtils.formatGraphicsUriForList(mActivity, mComment.getUser().avatar_url);
    }
}
