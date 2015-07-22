package com.soundcloud.android.sync.posts;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;
import java.util.Collection;

class RemovePostsCommand extends StoreCommand<Collection<PropertySet>> {

    @Inject
    RemovePostsCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        WriteResult writeResult = null;
        for (PropertySet post : input) {
            final Urn urn = post.get(PostProperty.TARGET_URN);
            writeResult = database.delete(Table.Posts, filter()
                    .whereEq(TableColumns.Posts.TARGET_ID, urn.getNumericId())
                    .whereEq(TableColumns.Posts.TARGET_TYPE, urn.isTrack()
                            ? TableColumns.Sounds.TYPE_TRACK
                            : TableColumns.Sounds.TYPE_PLAYLIST)
                    .whereEq(TableColumns.Posts.TYPE, post.get(PostProperty.IS_REPOST)
                            ? TableColumns.Posts.TYPE_REPOST
                            : TableColumns.Posts.TYPE_POST));
        }
        return writeResult; // not very robust, do we care about failure here?
    }
}

