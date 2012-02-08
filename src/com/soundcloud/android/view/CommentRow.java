package com.soundcloud.android.view;

import android.content.Context;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;

import com.soundcloud.android.Consts;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.SectionedCommentAdapter;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ImageUtils;

import java.util.Date;

public class CommentRow extends ActivityRow {
    private Comment mComment;
    private Track mTrack;

    public CommentRow(Context activity, LazyBaseAdapter adapter) {
        super(activity, adapter);
    }

    public CommentRow(Context mActivity, SectionedCommentAdapter sectionedCommentAdapter, Track track) {
        super(mActivity, sectionedCommentAdapter);
        mTrack = track;
    }

    @Override
    protected void init(){
        // do nothing
    }

    @Override
    protected boolean fillParcelable(Parcelable p){
        mComment = (Comment) p;
        return mComment != null;
    }

    @Override
    protected ActivityType getType() {
        return ActivityType.Comment;
    }

    @Override
    protected Track getTrack(){
        return mTrack == null ? mComment.track : mTrack;
    }

    @Override
    protected Comment getComment(){
        return mComment;
    }

    @Override
    protected User getOriginUser(){
        return mComment.user;
    }

    @Override
    protected Date getOriginCreatedAt(){
        return mComment.created_at;
    }

    @Override
    protected SpannableStringBuilder createSpan() {
        mSpanBuilder = new SpannableStringBuilder();
        mSpanBuilder.append("  \"").append(getComment().body).append("\"");
        return mSpanBuilder;
    }

    @Override
    public String getIconRemoteUri() {
        if (mComment == null || mComment.getUser() == null || mComment.getUser().avatar_url == null) return "";
        return Consts.GraphicSize.formatUriForList(getContext(), mComment.getUser().avatar_url);
    }
}
