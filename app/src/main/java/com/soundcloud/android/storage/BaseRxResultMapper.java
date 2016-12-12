package com.soundcloud.android.storage;

import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

import android.provider.BaseColumns;

public abstract class BaseRxResultMapper<T> extends RxResultMapper<T> {

    protected Urn readTrackUrn(CursorReader cursorReader) {
        return Urn.forTrack(cursorReader.getLong(BaseColumns._ID));
    }
}
