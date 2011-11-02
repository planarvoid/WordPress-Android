package com.soundcloud.android.view;

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

    public CommentSectionedRow(ScActivity activity, LazyBaseAdapter adapter) {
        super(activity, adapter);
    }

    public CommentSectionedRow(ScActivity mActivity, SectionedCommentAdapter sectionedCommentAdapter, Track track) {
        super(mActivity, sectionedCommentAdapter, track);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.activity_list_sectioned_row;
    }
}
