package com.soundcloud.android.playlists;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;

public class PlaylistInfoMapper extends PlaylistMapper {

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = super.map(cursorReader);

        propertySet.put(PlaylistProperty.CREATOR_URN, readCreatorUrn(cursorReader));
        propertySet.put(PlaylistProperty.DURATION, cursorReader.getInt(TableColumns.SoundView.DURATION));
        propertySet.put(PlaylistProperty.REPOSTS_COUNT, cursorReader.getInt(TableColumns.SoundView.REPOSTS_COUNT));
        propertySet.put(PlaylistProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.SoundView.CREATED_AT));
        propertySet.put(PlayableProperty.IS_LIKED, cursorReader.getBoolean(TableColumns.SoundView.USER_LIKE));
        propertySet.put(PlayableProperty.IS_REPOSTED, cursorReader.getBoolean(TableColumns.SoundView.USER_REPOST));
        propertySet.put(PlaylistProperty.IS_MARKED_FOR_OFFLINE, cursorReader.getBoolean(PlaylistMapper.IS_MARKED_FOR_OFFLINE));

        // we were not inserting this for a while, so we could have some remaining missing values. eventually this should always exist
        final String permalinkUrl = cursorReader.getString(TableColumns.SoundView.PERMALINK_URL);
        propertySet.put(PlaylistProperty.PERMALINK_URL, permalinkUrl != null ? permalinkUrl : ScTextUtils.EMPTY_STRING);
        return propertySet;
    }


}
