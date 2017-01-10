package com.soundcloud.android.presentation;

import com.soundcloud.android.model.Entity;
import com.soundcloud.android.tracks.TrackItem;

public interface UpdatableTrackItem extends Entity {
    UpdatableTrackItem updatedWithTrackItem(TrackItem trackItem);
}
