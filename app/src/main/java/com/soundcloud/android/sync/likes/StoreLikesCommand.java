package com.soundcloud.android.sync.likes;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class StoreLikesCommand extends StoreCommand<Collection<PropertySet>> {

    @Inject
    StoreLikesCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        List<ContentValues> values = new ArrayList<>(input.size());
        for (PropertySet like : input) {
            values.add(buildContentValuesForLike(like));
        }
        return database.bulkInsert(Table.Likes, values, SQLiteDatabase.CONFLICT_REPLACE);
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
