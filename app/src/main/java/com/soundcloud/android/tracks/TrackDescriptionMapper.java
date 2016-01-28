package com.soundcloud.android.tracks;

import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

final class TrackDescriptionMapper extends RxResultMapper<PropertySet> {
    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        final String description = cursorReader.getString(TableColumns.SoundView.DESCRIPTION);
        propertySet.put(TrackProperty.DESCRIPTION, description == null ? Strings.EMPTY : description);
        return propertySet;
    }
}
