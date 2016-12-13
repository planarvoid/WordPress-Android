package com.soundcloud.android.tracks;

import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

public class NewTrackItemMapper extends RxResultMapper<TrackItem> {

    private final TrackItemMapper trackItemMapper;

    public NewTrackItemMapper() {
        this.trackItemMapper = new TrackItemMapper();
    }


    @Override
    public TrackItem map(CursorReader reader) {
        return TrackItem.from(trackItemMapper.map(reader));
    }
}
