package com.soundcloud.android.playlists;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

import android.provider.BaseColumns;

public abstract class PlaylistMapper extends RxResultMapper<PropertySet> {

    public static final String LOCAL_TRACK_COUNT = "local_track_count";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        propertySet.put(PlaylistProperty.URN, readSoundUrn(cursorReader));
        propertySet.put(PlaylistProperty.TITLE, cursorReader.getString(Tables.PlaylistView.TITLE));
        propertySet.put(EntityProperty.IMAGE_URL_TEMPLATE, Optional.fromNullable(cursorReader.getString(Tables.PlaylistView.ARTWORK_URL)));
        propertySet.put(PlaylistProperty.CREATOR_NAME, cursorReader.getString(Tables.PlaylistView.USERNAME));
        propertySet.put(PlaylistProperty.TRACK_COUNT, readTrackCount(cursorReader));
        propertySet.put(PlaylistProperty.LIKES_COUNT, cursorReader.getInt(Tables.PlaylistView.LIKES_COUNT));
        propertySet.put(PlaylistProperty.IS_PRIVATE,
                        Sharing.PRIVATE.name()
                                       .equalsIgnoreCase(cursorReader.getString(Tables.PlaylistView.SHARING)));
        return propertySet;
    }

    static Urn readSoundUrn(CursorReader cursorReader) {
        return Urn.forPlaylist(cursorReader.getLong(Tables.PlaylistView.ID));
    }

    Urn readCreatorUrn(CursorReader cursorReader) {
        return Urn.forUser(cursorReader.getLong(Tables.PlaylistView.USER_ID));
    }

    static int readTrackCount(CursorReader cursorReader) {
        return Math.max(cursorReader.getInt(Tables.PlaylistView.LOCAL_TRACK_COUNT),
                        cursorReader.getInt(Tables.PlaylistView.TRACK_COUNT));
    }
}
