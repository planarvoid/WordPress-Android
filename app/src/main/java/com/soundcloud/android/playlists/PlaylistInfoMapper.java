package com.soundcloud.android.playlists;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;

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
        propertySet.put(PlaylistProperty.PLAY_DURATION, cursorReader.getLong(TableColumns.SoundView.DURATION));
        propertySet.put(PlaylistProperty.REPOSTS_COUNT, cursorReader.getInt(TableColumns.SoundView.REPOSTS_COUNT));
        propertySet.put(PlaylistProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.SoundView.CREATED_AT));
        propertySet.put(PlayableProperty.IS_USER_LIKE, cursorReader.getBoolean(TableColumns.SoundView.USER_LIKE));
        propertySet.put(PlayableProperty.IS_USER_REPOST, cursorReader.getBoolean(TableColumns.SoundView.USER_REPOST));
        propertySet.put(PlaylistProperty.IS_POSTED, creatorUrn.equals(loggedInUserUrn));

        // we were not inserting this for a while, so we could have some remaining missing values. eventually this should always exist
        final String permalinkUrl = cursorReader.getString(TableColumns.SoundView.PERMALINK_URL);
        propertySet.put(PlaylistProperty.PERMALINK_URL, permalinkUrl != null ? permalinkUrl : ScTextUtils.EMPTY_STRING);
        return propertySet;
    }


}
