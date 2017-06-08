package com.soundcloud.android.storage;

import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;
import com.soundcloud.propeller.schema.Column;

import android.provider.BaseColumns;

/**
 * <p>This class has been deprecated in favor of {@link BaseRxResultMapperV2<T>} and for the sake of migrating from RxJava 1 to
 * RxJava 2.</p>
 */
@Deprecated //Use BaseRxResultMapperV2 instead
public abstract class BaseRxResultMapper<T> extends RxResultMapper<T> {

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
