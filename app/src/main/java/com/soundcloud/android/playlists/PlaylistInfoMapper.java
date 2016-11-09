package com.soundcloud.android.playlists;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.Property;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.schema.Column;

public class PlaylistInfoMapper extends OfflinePlaylistMapper {

    private final Urn loggedInUserUrn;

    public PlaylistInfoMapper(Urn loggedInUserUrn) {
        this.loggedInUserUrn = loggedInUserUrn;
    }

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = super.map(cursorReader);

        final Urn creatorUrn = readCreatorUrn(cursorReader);
        propertySet.put(PlaylistProperty.CREATOR_URN, creatorUrn);
        propertySet.put(PlaylistProperty.PLAYLIST_DURATION, cursorReader.getLong(Tables.PlaylistView.DURATION));
        propertySet.put(PlaylistProperty.REPOSTS_COUNT, cursorReader.getInt(Tables.PlaylistView.REPOSTS_COUNT));
        propertySet.put(PlaylistProperty.CREATED_AT,
                        cursorReader.getDateFromTimestamp(Tables.PlaylistView.CREATED_AT));
        propertySet.put(PlayableProperty.IS_USER_LIKE, cursorReader.getBoolean(Tables.PlaylistView.IS_USER_LIKE));
        propertySet.put(PlayableProperty.IS_USER_REPOST, cursorReader.getBoolean(Tables.PlaylistView.IS_USER_REPOST));
        propertySet.put(PlaylistProperty.IS_POSTED, creatorUrn.equals(loggedInUserUrn));
        propertySet.put(PlaylistProperty.IS_ALBUM, cursorReader.getBoolean(Tables.PlaylistView.IS_ALBUM));
        putIfNotNull(cursorReader, propertySet, Tables.PlaylistView.SET_TYPE, PlaylistProperty.SET_TYPE);
        putIfNotNull(cursorReader, propertySet, Tables.PlaylistView.RELEASE_DATE, PlaylistProperty.RELEASE_DATE);

        // we were not inserting this for a while, so we could have some remaining missing values. eventually this should always exist
        final String permalinkUrl = cursorReader.getString(Tables.PlaylistView.PERMALINK_URL);
        propertySet.put(PlaylistProperty.PERMALINK_URL, permalinkUrl != null ? permalinkUrl : Strings.EMPTY);
        return propertySet;
    }

    private void putIfNotNull(CursorReader cursorReader, PropertySet propertySet,
                              Column column, Property<String> property) {
        if (cursorReader.isNotNull(column)) {
            propertySet.put(property, cursorReader.getString(column));
        }
    }
}
