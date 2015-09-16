package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.likes.TrackLikesActivity;

/***
 * TODO : Merge this with the existing TrackLikesScreen and tests when we go live with collections
 */

public class CollectionsTrackLikesScreen extends Screen {

    protected static final Class ACTIVITY = TrackLikesActivity.class;

    public CollectionsTrackLikesScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
