package com.soundcloud.android.sync.likes;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

class StoreLikesCommand extends DefaultWriteStorageCommand<Collection<PropertySet>, TxnResult> {

    @Inject
    StoreLikesCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, Collection<PropertySet> input) {
        List<ContentValues> values = new ArrayList<>(input.size());
        for (PropertySet like : input) {
            values.add(buildContentValuesForLike(like));
        }
        final HashMap<String, Class> columns = new HashMap<>();
        columns.put(TableColumns.Likes._ID, Long.class);
        columns.put(TableColumns.Likes._TYPE, Integer.class);
        columns.put(TableColumns.Likes.CREATED_AT, Long.class);
        return propeller.bulkInsert_experimental(Table.Likes, columns, values);
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
