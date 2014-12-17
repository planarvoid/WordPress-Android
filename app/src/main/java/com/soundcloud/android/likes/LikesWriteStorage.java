package com.soundcloud.android.likes;

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class LikesWriteStorage {

    private final PropellerDatabase propeller;

    @Inject
    LikesWriteStorage(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    TxnResult storeLikes(List<ApiLike> likes) {
        List<ContentValues> values = new ArrayList<>(likes.size());
        for (ApiLike like : likes) {
            values.add(buildContentValuesForLike(like));
        }
        return propeller.bulkInsert(Table.Likes, values);
    }

    private ContentValues buildContentValuesForLike(ApiLike like) {
        final ContentValues cv = new ContentValues();
        cv.put(TableColumns.Likes._ID, like.getUrn().getNumericId());
        cv.put(TableColumns.Likes._TYPE, like.getUrn().isTrack()
                ? TableColumns.Sounds.TYPE_TRACK
                : TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.Likes.CREATED_AT, like.getCreatedAt().getTime());
        return cv;
    }
}
