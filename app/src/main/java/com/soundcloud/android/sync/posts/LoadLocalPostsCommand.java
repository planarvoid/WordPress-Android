package com.soundcloud.android.sync.posts;

import static com.soundcloud.propeller.query.Query.Order.DESC;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.Posts;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class LoadLocalPostsCommand extends LegacyCommand<Object, List<PostRecord>, LoadLocalPostsCommand> {

    private final PropellerDatabase database;
    private final int resourceType;

    @Inject
    LoadLocalPostsCommand(PropellerDatabase database, int resourceType) {
        this.database = database;
        this.resourceType = resourceType;
    }

    @Override
    public List<PostRecord> call() throws Exception {
        return database.query(Query.from(Posts.TABLE)
                                   .select(Posts.TARGET_ID, Posts.CREATED_AT, Posts.TYPE)
                                   .whereEq(Posts.TARGET_TYPE, resourceType)
                                   .order(Posts.CREATED_AT, DESC))
                       .toList(new PlaylistMapper(resourceType == Tables.Sounds.TYPE_PLAYLIST));
    }

    private static class PlaylistMapper extends RxResultMapper<PostRecord> {
        private final boolean isPlaylist;

        private PlaylistMapper(boolean isPlaylist) {
            this.isPlaylist = isPlaylist;
        }

        @Override
        public PostRecord map(CursorReader cursorReader) {
            final boolean isRepost = Posts.TYPE_REPOST.equals(cursorReader.getString(Posts.TYPE));
            final Urn targetUrn = getUrn(cursorReader.getLong(Posts.TARGET_ID));
            final Date createdAt = new Date(cursorReader.getLong(Posts.CREATED_AT));
            return isRepost ? DatabasePostRecord.createRepost(targetUrn, createdAt) : DatabasePostRecord.createPost(targetUrn, createdAt);
        }

        private Urn getUrn(long id) {
            return isPlaylist ? Urn.forPlaylist(id) : Urn.forTrack(id);
        }
    }
}

