package com.soundcloud.android.likes;

import com.soundcloud.android.tracks.DownloadableTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.presentation.PagingItemAdapter;

import javax.inject.Inject;

class PagedTracksAdapter extends PagingItemAdapter<TrackItem> {

    private final TrackItemRenderer trackRenderer;

    @Inject
    PagedTracksAdapter(DownloadableTrackItemRenderer trackRenderer) {
        super(trackRenderer);
        this.trackRenderer = trackRenderer;
    }

    TrackItemRenderer getTrackRenderer() {
        return trackRenderer;
    }

}
