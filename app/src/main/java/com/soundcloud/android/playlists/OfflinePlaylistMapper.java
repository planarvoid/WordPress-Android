package com.soundcloud.android.playlists;

import com.soundcloud.android.offline.DownloadState;
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
            final DownloadState downloadState = cursorReader.getBoolean(HAS_PENDING_DOWNLOAD_REQUEST) ? DownloadState.REQUESTED : DownloadState.DOWNLOADED;
            propertySet.put(OfflineProperty.DOWNLOAD_STATE, downloadState);
        }
        return propertySet;
    }
}
