package com.soundcloud.android.playlists;

import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;

public abstract class OfflinePlaylistMapper extends PlaylistMapper {

    public static final String HAS_PENDING_DOWNLOAD_REQUEST = "has_pending_download_request";
    public static final String IS_MARKED_FOR_OFFLINE = "is_marked_for_offline";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = super.map(cursorReader);
        final boolean isMarkedForOffline = cursorReader.getBoolean(IS_MARKED_FOR_OFFLINE);
        propertySet.put(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE, isMarkedForOffline);
        if (isMarkedForOffline) {
            final OfflineState offlineState = cursorReader.getBoolean(HAS_PENDING_DOWNLOAD_REQUEST) ? OfflineState.REQUESTED : OfflineState.DOWNLOADED;
            propertySet.put(OfflineProperty.OFFLINE_STATE, offlineState);
        }
        return propertySet;
    }
}
