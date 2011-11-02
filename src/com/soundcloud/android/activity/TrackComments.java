package com.soundcloud.android.activity;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SectionedCommentAdapter;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.model.Comment;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.util.ArrayList;

public class TrackComments extends TrackInfoCollection {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    protected SectionedAdapter createSectionedAdapter() {
        return new SectionedCommentAdapter(this, mTrack);
    }

    @Override
    protected SectionedAdapter.Section createSection() {
        return new SectionedAdapter.Section(getString(R.string.list_header_track_comments),
                Comment.class, new ArrayList<Parcelable>(), Request.to(Endpoints.TRACK_COMMENTS, mTrack.id));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTrack != null) {
            trackPage(mTrack.pageTrack("comments"));
        }
    }


}
