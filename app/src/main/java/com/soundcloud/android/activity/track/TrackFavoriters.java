package com.soundcloud.android.activity.track;

import com.soundcloud.android.tracking.Page;

public class TrackFavoriters extends TrackInfoCollection {
    @Override
    public void onResume() {
        super.onResume();
        if (mTrack != null) {
            track(Page.Sounds_info__people_like, mTrack);
        }
    }
}