package com.soundcloud.android.tracks;

import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapperV2;

final class TrackDescriptionMapper extends RxResultMapperV2<Optional<String>> {
    @Override
    public Optional<String> map(CursorReader cursorReader) {
        final String description = cursorReader.getString(TableColumns.SoundView.DESCRIPTION);
        return Optional.of(description == null ? Strings.EMPTY : description);
    }
}
