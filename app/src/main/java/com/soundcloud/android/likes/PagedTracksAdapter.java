package com.soundcloud.android.likes;

import com.soundcloud.android.tracks.DownloadableTrackItemPresenter;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;

class PagedTracksAdapter extends PagingItemAdapter<PropertySet> {

    private final TrackItemPresenter trackPresenter;

    @Inject
    PagedTracksAdapter(DownloadableTrackItemPresenter trackPresenter) {
        super(trackPresenter);
        this.trackPresenter = trackPresenter;
    }

    TrackItemPresenter getTrackPresenter() {
        return trackPresenter;
    }

}
