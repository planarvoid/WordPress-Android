package com.soundcloud.android.view;

import android.content.Context;
import android.os.Parcelable;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.SectionedCommentAdapter;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ImageUtils;

import java.util.Date;

public class CommentSectionedRow extends CommentRow {
    private Comment mComment;
    private Track mTrack;

    public CommentSectionedRow(Context context, LazyBaseAdapter adapter) {
        super(context, adapter);
    }

    public CommentSectionedRow(Context context, SectionedCommentAdapter sectionedCommentAdapter, Track track) {
        super(context, sectionedCommentAdapter, track);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.activity_list_sectioned_row;
    }
}
