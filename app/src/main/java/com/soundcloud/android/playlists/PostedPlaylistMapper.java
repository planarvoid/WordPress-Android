package com.soundcloud.android.playlists;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.rx.RxResultMapper;

import android.provider.BaseColumns;

public class PostedPlaylistMapper extends RxResultMapper<PropertySet> {

    private static final String SHARING_PRIVATE = "private";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

        propertySet.put(PlaylistProperty.URN, readSoundUrn(cursorReader));
        propertySet.put(PlaylistProperty.TITLE, cursorReader.getString(TableColumns.SoundView.TITLE));
        propertySet.put(PlaylistProperty.CREATOR_NAME, cursorReader.getString(TableColumns.SoundView.USERNAME));
        propertySet.put(PlaylistProperty.TRACK_COUNT, cursorReader.getInt(TableColumns.SoundView.TRACK_COUNT));
        propertySet.put(PlaylistProperty.LIKES_COUNT, cursorReader.getInt(TableColumns.SoundView.LIKES_COUNT));
        propertySet.put(PlaylistProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.SoundView.CREATED_AT));
        propertySet.put(PlaylistProperty.IS_PRIVATE, SHARING_PRIVATE.equalsIgnoreCase(cursorReader.getString(TableColumns.SoundView.SHARING)));
        return propertySet;
    }

    private Urn readSoundUrn(CursorReader cursorReader) {
        return Urn.forPlaylist(cursorReader.getLong(BaseColumns._ID));
    }

}
