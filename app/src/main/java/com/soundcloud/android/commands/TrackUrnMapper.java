package com.soundcloud.android.commands;

import static android.provider.BaseColumns._ID;

import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

public final class TrackUrnMapper extends RxResultMapper<Urn> {
    @Override
    public Urn map(CursorReader cursorReader) {
        return Urn.forTrack(cursorReader.getLong(_ID));
    }
}
