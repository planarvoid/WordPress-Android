package com.soundcloud.android.sync.posts;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;
import java.util.Collection;

class RemovePostsCommand extends DefaultWriteStorageCommand<Collection<PropertySet>, WriteResult> {

    @Inject
    RemovePostsCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, Collection<PropertySet> input) {
        WriteResult writeResult = null;
        for (PropertySet post : input) {
            final Urn urn = post.get(PostProperty.TARGET_URN);
            writeResult = propeller.delete(Tables.Posts.TABLE, filter()
                    .whereEq(Tables.Posts.TARGET_ID, urn.getNumericId())
                    .whereEq(Tables.Posts.TARGET_TYPE, urn.isTrack()
                                                             ? Tables.Sounds.TYPE_TRACK
                                                             : Tables.Sounds.TYPE_PLAYLIST)
                    .whereEq(Tables.Posts.TYPE, post.get(PostProperty.IS_REPOST)
                                                      ? Tables.Posts.TYPE_REPOST
                                                      : Tables.Posts.TYPE_POST));
        }
        return writeResult; // not very robust, do we care about failure here?
    }
}

