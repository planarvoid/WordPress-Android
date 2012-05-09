package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.*;

public class SectionedCommentAdapter extends SectionedAdapter {

    private Track mTrack;

    public SectionedCommentAdapter(ScListActivity activity, Track track) {
        super(activity);
        mTrack = track;
    }

    @Override protected LazyRow createRow(int position) {
        return new CommentSectionedRow(mContext, this, mTrack);
    }
}
