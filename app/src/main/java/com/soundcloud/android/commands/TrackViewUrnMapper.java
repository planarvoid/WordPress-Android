package com.soundcloud.android.commands;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

public final class TrackViewUrnMapper extends RxResultMapper<Urn> {
    @Override
    public Urn map(CursorReader cursorReader) {
        return Urn.forTrack(cursorReader.getLong(Tables.TrackView.ID.name()));
    }
}
