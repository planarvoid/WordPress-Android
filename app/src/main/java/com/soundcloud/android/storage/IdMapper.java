package com.soundcloud.android.storage;

import static android.provider.BaseColumns._ID;

import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

public class IdMapper extends RxResultMapper<Long> {

    @Override
    public Long map(CursorReader reader) {
        return reader.getLong(_ID);
    }
}
