package com.soundcloud.android.likes;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.WhereBuilder;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LikesWriteStorage {

    private final PropellerDatabase propeller;

    @Inject
    LikesWriteStorage(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    public TxnResult storeLikes(Collection<PropertySet> likes) {
        List<ContentValues> values = new ArrayList<>(likes.size());
        for (PropertySet like : likes) {
            values.add(buildContentValuesForLike(like));
        }
        return propeller.bulkInsert(Table.Likes, values);
    }

    public ChangeResult removeLikes(Collection<PropertySet> likes) {
        List<Long> ids = new ArrayList<>(likes.size());
        for (PropertySet like : likes) {
            ids.add(like.get(LikeProperty.TARGET_URN).getNumericId());
        }
        return propeller.delete(Table.Likes, new WhereBuilder().whereIn(TableColumns.Likes._ID, ids));
    }

    private ContentValues buildContentValuesForLike(PropertySet like) {
        final ContentValues cv = new ContentValues();
        final Urn targetUrn = like.get(LikeProperty.TARGET_URN);
        cv.put(TableColumns.Likes._ID, targetUrn.getNumericId());
        cv.put(TableColumns.Likes._TYPE, targetUrn.isTrack()
                ? TableColumns.Sounds.TYPE_TRACK
                : TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.Likes.CREATED_AT, like.get(LikeProperty.CREATED_AT).getTime());
        return cv;
    }
}
