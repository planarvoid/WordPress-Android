package com.soundcloud.android.tracks;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;

public interface PlayingTrackRenderer extends CellRenderer<TrackItem> {
    void setPlayingTrack(Urn currentTrackUrn);
}
