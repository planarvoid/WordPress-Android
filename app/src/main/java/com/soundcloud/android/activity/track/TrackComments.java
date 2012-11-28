package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.tracking.Page;

import android.net.Uri;
import android.os.Bundle;

public class TrackComments extends TrackInfoCollection {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.list_header_track_comments);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTrack != null) {
            track(Page.Sounds_info__comment, mTrack);
        }
    }

    @Override
    protected int getSelectedMenuId() {
        return -1;
    }

    @Override
    protected Uri getContentUri() {
        return Content.TRACK_COMMENTS.forId(mTrack.id);
    }
}
