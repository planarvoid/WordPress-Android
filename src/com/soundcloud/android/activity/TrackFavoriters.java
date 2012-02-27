package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedUserlistAdapter;
import com.soundcloud.android.model.User;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.os.Parcelable;

import java.util.ArrayList;

public class TrackFavoriters extends TrackInfoCollection {
    @Override
    protected SectionedAdapter createSectionedAdapter() {
        return new SectionedUserlistAdapter(this);
    }

    @Override
    protected SectionedAdapter.Section createSection() {
        return new SectionedAdapter.Section(R.string.list_header_track_favoriters,
                User.class, new ArrayList<Parcelable>(), null, Request.to(Endpoints.TRACK_FAVORITERS, mTrack.id));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTrack != null) {
            track(Page.Sounds_info__people_like, mTrack);
        }
    }
}
