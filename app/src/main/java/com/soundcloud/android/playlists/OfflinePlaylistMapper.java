package com.soundcloud.android.playlists;

import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;

public abstract class OfflinePlaylistMapper extends PlaylistMapper {

    public static final String HAS_PENDING_DOWNLOAD_REQUEST = "has_pending_download_request";
    public static final String HAS_OFFLINE_TRACKS = "has_offline_tracks";
    public static final String IS_MARKED_FOR_OFFLINE = "is_marked_for_offline";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = super.map(cursorReader);
        final boolean isMarkedForOffline = cursorReader.getBoolean(IS_MARKED_FOR_OFFLINE);

        propertySet.put(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE, isMarkedForOffline);
        if (isMarkedForOffline) {

            OfflineState offlineState;
            if (cursorReader.getBoolean(HAS_PENDING_DOWNLOAD_REQUEST)) {
                offlineState = OfflineState.REQUESTED;
            } else if (cursorReader.getBoolean(HAS_OFFLINE_TRACKS)) {
                offlineState = OfflineState.DOWNLOADED;
            } else {
                offlineState = OfflineState.UNAVAILABLE;
            }

            propertySet.put(OfflineProperty.OFFLINE_STATE, offlineState);
        }
        return propertySet;
    }
}
