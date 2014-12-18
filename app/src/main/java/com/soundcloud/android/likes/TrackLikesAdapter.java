package com.soundcloud.android.likes;

import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;

public class TrackLikesAdapter extends EndlessAdapter<PropertySet> {

    @Inject
    public TrackLikesAdapter(TrackItemPresenter trackPresenter) {
        super(trackPresenter);
    }

}
