package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.storage.TableColumns.CollectionItems;
import static com.soundcloud.android.storage.TableColumns.Posts;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class LoadLocalPostsCommand extends Command<Object, List<PropertySet>, LoadLocalPostsCommand> {

    private final PropellerDatabase database;
    private final int resourceType;

    @Inject
    LoadLocalPostsCommand(PropellerDatabase database, int resourceType) {
        this.database = database;
        this.resourceType = resourceType;
    }

    @Override
    public List<PropertySet> call() throws Exception {
        return database.query(Query.from(Table.Posts.name())
                .select(Posts._ID, Posts.CREATED_AT, Posts.IS_REPOST)
                .whereEq(Posts._TYPE, resourceType)
                .order(CollectionItems.CREATED_AT, Query.ORDER_DESC)).toList(new PlaylistMapper());
    }

    private static class PlaylistMapper extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            return PropertySet.from(
                    PostProperty.TARGET_URN.bind(Urn.forPlaylist(cursorReader.getLong(Posts._ID))),
                    PostProperty.CREATED_AT.bind(new Date(cursorReader.getLong(Posts.CREATED_AT))),
                    PostProperty.IS_REPOST.bind(cursorReader.getBoolean(Posts.IS_REPOST))
            );
        }
    }
}

