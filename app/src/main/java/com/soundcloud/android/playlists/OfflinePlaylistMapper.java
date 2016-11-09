package com.soundcloud.android.playlists;

import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;

public abstract class OfflinePlaylistMapper extends PlaylistMapper {

    public static final String HAS_PENDING_DOWNLOAD_REQUEST = "has_pending_download_request";
    public static final String HAS_DOWNLOADED_TRACKS = "has_downloaded_tracks";
    public static final String HAS_UNAVAILABLE_TRACKS = "has_unavailable_tracks";
    public static final String IS_MARKED_FOR_OFFLINE = "is_marked_for_offline";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = super.map(cursorReader);
        final boolean isMarkedForOffline = cursorReader.getBoolean(Tables.PlaylistView.IS_MARKED_FOR_OFFLINE);

        propertySet.put(OfflineProperty.IS_MARKED_FOR_OFFLINE, isMarkedForOffline);
        if (isMarkedForOffline) {
            propertySet.put(OfflineProperty.OFFLINE_STATE, OfflineState.getOfflineState(
                    cursorReader.getBoolean(Tables.PlaylistView.HAS_PENDING_DOWNLOAD_REQUEST),
                    cursorReader.getBoolean(Tables.PlaylistView.HAS_DOWNLOADED_TRACKS),
                    cursorReader.getBoolean(Tables.PlaylistView.HAS_UNAVAILABLE_TRACKS)));
        }
        return propertySet;
    }

}
