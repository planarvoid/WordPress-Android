package com.soundcloud.android.commands;

import static android.provider.BaseColumns._ID;

import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

public final class PlaylistUrnMapper extends RxResultMapper<Urn> {
    @Override
    public Urn map(CursorReader cursorReader) {
        return Urn.forPlaylist(cursorReader.getLong(_ID));
    }
}