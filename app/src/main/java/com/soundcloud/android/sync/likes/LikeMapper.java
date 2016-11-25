package com.soundcloud.android.sync.likes;

import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

class LikeMapper extends RxResultMapper<PropertySet> {
    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet like = PropertySet.create(cursorReader.getColumnCount());
        if (cursorReader.getInt(Tables.Likes._TYPE) == Tables.Sounds.TYPE_TRACK) {
            like.put(LikeProperty.TARGET_URN, Urn.forTrack(cursorReader.getLong(Tables.Likes._ID)));
        } else if (cursorReader.getInt(Tables.Likes._TYPE) == Tables.Sounds.TYPE_PLAYLIST) {
            like.put(LikeProperty.TARGET_URN, Urn.forPlaylist(cursorReader.getLong(Tables.Likes._ID)));
        }

        like.put(LikeProperty.CREATED_AT, cursorReader.getDateFromTimestamp(Tables.Likes.CREATED_AT));
        if (cursorReader.isNotNull(Tables.Likes.ADDED_AT)) {
            like.put(LikeProperty.ADDED_AT, cursorReader.getDateFromTimestamp(Tables.Likes.ADDED_AT));
        }
        if (cursorReader.isNotNull(Tables.Likes.REMOVED_AT)) {
            like.put(LikeProperty.REMOVED_AT, cursorReader.getDateFromTimestamp(Tables.Likes.REMOVED_AT));
        }

        return like;
    }
}
