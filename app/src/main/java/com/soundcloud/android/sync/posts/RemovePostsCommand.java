package com.soundcloud.android.sync.posts;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.WhereBuilder;

import java.util.Collection;

public class RemovePostsCommand extends StoreCommand<Collection<PropertySet>> {

    RemovePostsCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        WriteResult writeResult = null;
        for (PropertySet post : input) {
            final Urn urn = post.get(PostProperty.TARGET_URN);
            writeResult = database.delete(Table.Posts, new WhereBuilder()
                    .whereEq(TableColumns.Posts._ID, urn.getNumericId())
                    .whereEq(TableColumns.Posts.IS_REPOST, post.get(PostProperty.IS_REPOST))
                    .whereEq(TableColumns.Posts._TYPE, urn.isTrack()
                            ? TableColumns.Sounds.TYPE_TRACK
                            : TableColumns.Sounds.TYPE_PLAYLIST));
        }
        return writeResult; // not very robust, do we care about failure here?
    }
}

