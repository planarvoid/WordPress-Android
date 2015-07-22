package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.storage.TableColumns.CollectionItems.CREATED_AT;
import static com.soundcloud.android.storage.TableColumns.Posts;
import static com.soundcloud.propeller.query.Query.Order.DESC;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class LoadLocalPostsCommand extends LegacyCommand<Object, List<PropertySet>, LoadLocalPostsCommand> {

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
                .select(Posts.TARGET_ID, Posts.CREATED_AT, Posts.TYPE)
                .whereEq(Posts.TARGET_TYPE, resourceType)
                .order(CREATED_AT, DESC)).toList(new PlaylistMapper(resourceType == TableColumns.Sounds.TYPE_PLAYLIST));
    }

    private static class PlaylistMapper extends RxResultMapper<PropertySet> {
        private final boolean isPlaylist;

        private PlaylistMapper(boolean isPlaylist) {
            this.isPlaylist = isPlaylist;
        }

        @Override
        public PropertySet map(CursorReader cursorReader) {
            return PropertySet.from(
                    PostProperty.TARGET_URN.bind(getUrn(cursorReader.getLong(Posts.TARGET_ID))),
                    PostProperty.CREATED_AT.bind(new Date(cursorReader.getLong(Posts.CREATED_AT))),
                    PostProperty.IS_REPOST.bind(Posts.TYPE_REPOST.equals(cursorReader.getString(Posts.TYPE)))
            );
        }

        private Urn getUrn(long id) {
            return isPlaylist ? Urn.forPlaylist(id) : Urn.forTrack(id);
        }
    }
}

