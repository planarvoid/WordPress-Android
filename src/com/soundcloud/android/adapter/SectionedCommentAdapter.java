package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.*;

public class SectionedCommentAdapter extends SectionedAdapter {

    private Track mTrack;

    public SectionedCommentAdapter(ScActivity activity, Track track) {
        super(activity);
        mTrack = track;
    }

    @Override protected LazyRow createRow(int position) {
        return new CommentSectionedRow(mActivity, this, mTrack);
    }
}
