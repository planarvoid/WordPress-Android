package com.soundcloud.android.playlists;

import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;

public class PostedPlaylistMapper extends OfflinePlaylistMapper {

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = super.map(cursorReader);
        propertySet.put(PlaylistProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.SoundView.CREATED_AT));
        return propertySet;
    }
}
