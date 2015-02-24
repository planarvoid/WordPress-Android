package com.soundcloud.android.sync.posts;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StorePostsCommand extends StoreCommand<Collection<PropertySet>> {

    @Inject
    StorePostsCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        List<ContentValues> values = new ArrayList<>(input.size());
        for (PropertySet playlistPost : input) {
            values.add(buildContentValuesForPlaylistPost(playlistPost));
        }
        return database.bulkInsert(Table.Posts, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private ContentValues buildContentValuesForPlaylistPost(PropertySet playlistPost) {
        final ContentValues cv = new ContentValues();
        final Urn targetUrn = playlistPost.get(PostProperty.TARGET_URN);
        cv.put(TableColumns.Posts._ID, targetUrn.getNumericId());
        cv.put(TableColumns.Posts._TYPE, targetUrn.isTrack()
                ? TableColumns.Sounds.TYPE_TRACK
                : TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.Posts.IS_REPOST, playlistPost.get(PostProperty.IS_REPOST));
        cv.put(TableColumns.Posts.CREATED_AT, playlistPost.get(PostProperty.CREATED_AT).getTime());
        return cv;
    }
}