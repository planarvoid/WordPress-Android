package com.soundcloud.android.likes;

import com.soundcloud.android.tracks.SyncableTrackItemPresenter;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;

class PagedTracksAdapter extends EndlessAdapter<PropertySet> {

    private final TrackItemPresenter trackPresenter;

    @Inject
    PagedTracksAdapter(SyncableTrackItemPresenter trackPresenter) {
        super(trackPresenter);
        this.trackPresenter = trackPresenter;
    }

    TrackItemPresenter getTrackPresenter() {
        return trackPresenter;
    }

}
