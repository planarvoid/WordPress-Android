package com.soundcloud.android.sync.posts;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.Posts;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.schema.BulkInsertValues;
import com.soundcloud.propeller.schema.Column;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class StorePostsCommand extends DefaultWriteStorageCommand<Collection<PropertySet>, WriteResult> {

    @Inject
    StorePostsCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, Collection<PropertySet> input) {
        BulkInsertValues.Builder builder = new BulkInsertValues.Builder(getColumns());
        for (PropertySet post : input) {
           builder.addRow(buildRowForPost(post));
        }
        return propeller.bulkInsert(Posts.TABLE, builder.build());
    }

    @NonNull
    private List<Column> getColumns() {
        return Arrays.asList(
                Posts.TARGET_ID,
                Posts.TARGET_TYPE,
                Posts.TYPE,
                Posts.CREATED_AT
        );
    }

    private List<Object> buildRowForPost(PropertySet post) {
        final Urn targetUrn = post.get(PostProperty.TARGET_URN);
        return Arrays.<Object>asList(
                targetUrn.getNumericId(),
                targetUrn.isTrack()
                ? Tables.Sounds.TYPE_TRACK
                : Tables.Sounds.TYPE_PLAYLIST,
                post.get(PostProperty.IS_REPOST) ? Posts.TYPE_REPOST : Posts.TYPE_POST,
                post.get(PostProperty.CREATED_AT).getTime()
        );
    }
}
