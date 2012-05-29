package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedCommentAdapter;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.os.Parcelable;

import java.util.ArrayList;

public class TrackComments extends TrackInfoCollection {
    @Override
    protected SectionedAdapter createSectionedAdapter() {
        return new SectionedCommentAdapter(this, mTrack);
    }

    @Override
    protected SectionedAdapter.Section createSection() {
        return new SectionedAdapter.Section(R.string.list_header_track_comments,
                Comment.class, new ArrayList<Parcelable>(), null, Request.to(Endpoints.TRACK_COMMENTS, mTrack.id));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTrack != null) {
            track(Page.Sounds_info__comment, mTrack);
        }
    }
}
