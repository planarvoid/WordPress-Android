package com.soundcloud.android.playlists;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;

public class NewPlaylistMapper extends RxResultMapper<PlaylistItem> {

    @Inject
    public NewPlaylistMapper() {
    }

    @Override
    public PlaylistItem map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        propertySet.put(PlaylistProperty.URN, Urn.forPlaylist(cursorReader.getLong(Tables.PlaylistView.ID)));
        propertySet.put(PlaylistProperty.TITLE, cursorReader.getString(Tables.PlaylistView.TITLE));
        propertySet.put(EntityProperty.IMAGE_URL_TEMPLATE, Optional.fromNullable(cursorReader.getString(Tables.PlaylistView.ARTWORK_URL)));
        propertySet.put(PlaylistProperty.CREATOR_URN, Urn.forUser(cursorReader.getLong(Tables.PlaylistView.USER_ID)));
        propertySet.put(PlaylistProperty.CREATOR_NAME, cursorReader.getString(Tables.PlaylistView.USERNAME));
        propertySet.put(PlaylistProperty.TRACK_COUNT, readTrackCount(cursorReader));
        propertySet.put(PlaylistProperty.LIKES_COUNT, cursorReader.getInt(Tables.PlaylistView.LIKES_COUNT));
        propertySet.put(PlaylistProperty.IS_PRIVATE, readIsPrivate(cursorReader));
        propertySet.put(PlayableProperty.IS_USER_LIKE, cursorReader.getBoolean(Tables.PlaylistView.IS_USER_LIKE));
        final boolean isMarkedForOffline = cursorReader.getBoolean(Tables.PlaylistView.IS_MARKED_FOR_OFFLINE);
        propertySet.put(OfflineProperty.IS_MARKED_FOR_OFFLINE, isMarkedForOffline);
        if (isMarkedForOffline) {
            propertySet.put(OfflineProperty.OFFLINE_STATE, OfflineState.getOfflineState(
                    cursorReader.getBoolean(Tables.PlaylistView.HAS_PENDING_DOWNLOAD_REQUEST),
                    cursorReader.getBoolean(Tables.PlaylistView.HAS_DOWNLOADED_TRACKS),
                    cursorReader.getBoolean(Tables.PlaylistView.HAS_UNAVAILABLE_TRACKS)));
        }
        return new PlaylistItem(propertySet);
    }

    public boolean readIsPrivate(CursorReader cursorReader) {
        return Sharing.PRIVATE.name().equalsIgnoreCase(cursorReader.getString(Tables.PlaylistView.SHARING));
    }

    static int readTrackCount(CursorReader cursorReader) {
        return Math.max(cursorReader.getInt(Tables.PlaylistView.LOCAL_TRACK_COUNT),
                        cursorReader.getInt(Tables.PlaylistView.TRACK_COUNT));
    }

}
