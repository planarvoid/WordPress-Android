package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.storage.TableColumns.Posts;

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
        cv.put(Posts.TARGET_ID, targetUrn.getNumericId());
        cv.put(Posts.TARGET_TYPE, targetUrn.isTrack()
                ? TableColumns.Sounds.TYPE_TRACK
                : TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(Posts.TYPE, playlistPost.get(PostProperty.IS_REPOST) ? Posts.TYPE_REPOST : Posts.TYPE_POST);
        cv.put(Posts.CREATED_AT, playlistPost.get(PostProperty.CREATED_AT).getTime());
        return cv;
    }
}