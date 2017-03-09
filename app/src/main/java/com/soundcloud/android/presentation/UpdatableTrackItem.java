package com.soundcloud.android.presentation;

import com.soundcloud.android.model.Entity;
import com.soundcloud.android.tracks.Track;

public interface UpdatableTrackItem extends Entity {
    UpdatableTrackItem updatedWithTrack(Track track);
}
