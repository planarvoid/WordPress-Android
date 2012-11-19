package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.tracking.Page;

import android.net.Uri;
import android.os.Bundle;

public class TrackReposters extends TrackInfoCollection {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(R.string.list_header_track_reposters);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTrack != null) {
            track(Page.Sounds_info__people_like, mTrack);
        }
    }

    @Override
    protected int getSelectedMenuId() {
        return -1;
    }

    @Override
    protected Uri getContentUri() {
        return Content.TRACK_REPOSTERS.forId(mTrack.id);
    }
}