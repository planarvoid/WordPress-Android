package com.soundcloud.android.sync.likes;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

import java.util.Date;

class LikeMapper extends RxResultMapper<LikeRecord> {
    @Override
    public LikeRecord map(CursorReader cursorReader) {
        Urn targetUrn = Urn.NOT_SET;
        if (cursorReader.getInt(Tables.Likes._TYPE) == Tables.Sounds.TYPE_TRACK) {
            targetUrn = Urn.forTrack(cursorReader.getLong(Tables.Likes._ID));
        } else if (cursorReader.getInt(Tables.Likes._TYPE) == Tables.Sounds.TYPE_PLAYLIST) {
            targetUrn = Urn.forPlaylist(cursorReader.getLong(Tables.Likes._ID));
        }
        final Date createdAt = cursorReader.getDateFromTimestamp(Tables.Likes.CREATED_AT);
        return DatabaseLikeRecord.create(targetUrn, createdAt);
    }
}
