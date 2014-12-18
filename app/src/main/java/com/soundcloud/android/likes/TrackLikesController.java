package com.soundcloud.android.likes;

import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.propeller.PropertySet;

public class TrackLikesController {

    private final TrackItemPresenter trackPresenter;
    private final ItemAdapter<PropertySet> adapter;

    public TrackLikesController(TrackItemPresenter trackPresenter, ItemAdapter<PropertySet> adapter) {
        this.trackPresenter = trackPresenter;
        this.adapter = adapter;
    }

    ItemAdapter<PropertySet> getAdapter() {
        return adapter;
    }


}
