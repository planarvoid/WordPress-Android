package com.soundcloud.android.playlists;

import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.rx.RxResultMapper;

import android.provider.BaseColumns;

public class PlaylistItemMapper extends RxResultMapper<PropertySet> {

    public static final String IS_MARKED_FOR_OFFLINE = "is_marked_for_offline";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        propertySet.put(PlaylistProperty.URN, readSoundUrn(cursorReader));
        propertySet.put(PlaylistProperty.TITLE, cursorReader.getString(TableColumns.SoundView.TITLE));
        propertySet.put(PlaylistProperty.CREATOR_NAME, cursorReader.getString(TableColumns.SoundView.USERNAME));
        propertySet.put(PlaylistProperty.CREATOR_URN, readCreatorUrn(cursorReader));
        propertySet.put(PlaylistProperty.DURATION, cursorReader.getInt(TableColumns.SoundView.DURATION));
        propertySet.put(PlaylistProperty.TRACK_COUNT, cursorReader.getInt(TableColumns.SoundView.TRACK_COUNT));
        propertySet.put(PlaylistProperty.LIKES_COUNT, cursorReader.getInt(TableColumns.SoundView.LIKES_COUNT));
        propertySet.put(PlaylistProperty.REPOSTS_COUNT, cursorReader.getInt(TableColumns.SoundView.REPOSTS_COUNT));
        propertySet.put(PlaylistProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.SoundView.CREATED_AT));
        propertySet.put(PlaylistProperty.IS_PRIVATE, Sharing.PRIVATE.name().equalsIgnoreCase(
                cursorReader.getString(TableColumns.SoundView.SHARING)));
        propertySet.put(PlayableProperty.IS_LIKED, cursorReader.getBoolean(TableColumns.SoundView.USER_LIKE));
        propertySet.put(PlayableProperty.IS_REPOSTED, cursorReader.getBoolean(TableColumns.SoundView.USER_REPOST));
        propertySet.put(PlaylistProperty.IS_MARKED_FOR_OFFLINE, cursorReader.getBoolean(IS_MARKED_FOR_OFFLINE));

        // we were not inserting this for a while, so we could have some remaining missing values. eventually this should always exist
        final String permalinkUrl = cursorReader.getString(TableColumns.SoundView.PERMALINK_URL);
        propertySet.put(PlaylistProperty.PERMALINK_URL, permalinkUrl != null ? permalinkUrl : ScTextUtils.EMPTY_STRING);
        return propertySet;
    }

    private Urn readSoundUrn(CursorReader cursorReader) {
        return Urn.forPlaylist(cursorReader.getLong(BaseColumns._ID));
    }

    private Urn readCreatorUrn(CursorReader cursorReader) {
        return Urn.forUser(cursorReader.getLong(TableColumns.SoundView.USER_ID));
    }
}
