package com.soundcloud.android.playlists;

import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;

public class LikedPlaylistMapper extends OfflinePlaylistMapper {

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = super.map(cursorReader);
        propertySet.put(LikeProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.Likes.CREATED_AT));
        propertySet.put(PlaylistProperty.IS_LIKED, true);
        return propertySet;
    }
}
