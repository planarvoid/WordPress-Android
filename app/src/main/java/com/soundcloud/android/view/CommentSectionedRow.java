package com.soundcloud.android.view;

import android.content.Context;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.adapter.SectionedCommentAdapter;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;

public class CommentSectionedRow extends CommentRow {
    private Comment mComment;
    private Track mTrack;

    public CommentSectionedRow(Context context, ScBaseAdapter adapter) {
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
