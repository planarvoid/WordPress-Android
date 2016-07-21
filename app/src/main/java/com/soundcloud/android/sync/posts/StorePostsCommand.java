package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.storage.TableColumns.Posts;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.model.PostProperty;
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

public class StorePostsCommand extends DefaultWriteStorageCommand<Collection<PropertySet>, WriteResult> {

    @Inject
    StorePostsCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, Collection<PropertySet> input) {
        List<ContentValues> values = new ArrayList<>(input.size());
        for (PropertySet post : input) {
            values.add(buildContentValuesForPost(post));
        }
        return propeller.bulkInsert(Table.Posts, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private ContentValues buildContentValuesForPost(PropertySet post) {
        final ContentValues cv = new ContentValues();
        final Urn targetUrn = post.get(PostProperty.TARGET_URN);
        cv.put(Posts.TARGET_ID, targetUrn.getNumericId());
        cv.put(Posts.TARGET_TYPE, targetUrn.isTrack()
                                  ? TableColumns.Sounds.TYPE_TRACK
                                  : TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(Posts.TYPE, post.get(PostProperty.IS_REPOST) ? Posts.TYPE_REPOST : Posts.TYPE_POST);
        cv.put(Posts.CREATED_AT, post.get(PostProperty.CREATED_AT).getTime());
        return cv;
    }
}
