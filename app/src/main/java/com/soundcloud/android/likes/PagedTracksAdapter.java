package com.soundcloud.android.likes;

import com.soundcloud.android.tracks.DownloadableTrackItemPresenter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.view.adapters.PagingItemAdapter;

import javax.inject.Inject;

class PagedTracksAdapter extends PagingItemAdapter<TrackItem> {

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
