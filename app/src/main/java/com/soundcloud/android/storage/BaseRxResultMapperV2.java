package com.soundcloud.android.storage;

import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapperV2;
import com.soundcloud.propeller.schema.Column;

import android.provider.BaseColumns;

public abstract class BaseRxResultMapperV2<T> extends RxResultMapperV2<T> {

    protected Urn readTrackUrn(CursorReader cursorReader) {
        return Urn.forTrack(cursorReader.getLong(BaseColumns._ID));
    }

    protected static Urn readSoundUrn(CursorReader cursorReader, Column idColumn, Column typeColumn) {
        final int soundId = cursorReader.getInt(idColumn);
        return getSoundType(cursorReader, typeColumn) == Tables.Sounds.TYPE_TRACK ? Urn.forTrack(soundId) : Urn.forPlaylist(soundId);
    }

    private static int getSoundType(CursorReader cursorReader, Column typeColumn) {
        return cursorReader.getInt(typeColumn);
    }
}
