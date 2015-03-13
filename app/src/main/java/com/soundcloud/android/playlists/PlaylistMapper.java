package com.soundcloud.android.playlists;

import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.rx.RxResultMapper;

import android.provider.BaseColumns;

public abstract class PlaylistMapper extends RxResultMapper<PropertySet> {

    public static final String IS_MARKED_FOR_OFFLINE = "is_marked_for_offline";
    public static final String LOCAL_TRACK_COUNT = "local_track_count";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        propertySet.put(PlaylistProperty.URN, readSoundUrn(cursorReader));
        propertySet.put(PlaylistProperty.TITLE, cursorReader.getString(TableColumns.SoundView.TITLE));
        propertySet.put(PlaylistProperty.CREATOR_NAME, cursorReader.getString(TableColumns.SoundView.USERNAME));
        propertySet.put(PlaylistProperty.TRACK_COUNT, getTrackCount(cursorReader));
        propertySet.put(PlaylistProperty.LIKES_COUNT, cursorReader.getInt(TableColumns.SoundView.LIKES_COUNT));
        propertySet.put(PlaylistProperty.IS_PRIVATE, Sharing.PRIVATE.name().equalsIgnoreCase(cursorReader.getString(TableColumns.SoundView.SHARING)));
        return propertySet;
    }

    public Urn readSoundUrn(CursorReader cursorReader) {
        return Urn.forPlaylist(cursorReader.getLong(BaseColumns._ID));
    }

    public Urn readCreatorUrn(CursorReader cursorReader) {
        return Urn.forUser(cursorReader.getLong(TableColumns.SoundView.USER_ID));
    }

    public static int getTrackCount(CursorReader cursorReader) {
        return Math.max(cursorReader.getInt(PlaylistMapper.LOCAL_TRACK_COUNT),
                cursorReader.getInt(TableColumns.SoundView.TRACK_COUNT));
    }
}
