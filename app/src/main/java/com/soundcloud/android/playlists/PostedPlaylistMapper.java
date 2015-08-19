package com.soundcloud.android.playlists;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;

public class PostedPlaylistMapper extends OfflinePlaylistMapper {

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = super.map(cursorReader);
        propertySet.put(PostProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.Posts.CREATED_AT));
        propertySet.put(PlayableProperty.IS_LIKED, cursorReader.getBoolean(TableColumns.SoundView.USER_LIKE));
        return propertySet;
    }
}
