package com.soundcloud.android.discovery.recommendedplaylists;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StoreRecommendedPlaylistsCommand
        extends DefaultWriteStorageCommand<ModelCollection<ApiRecommendedPlaylistBucket>, WriteResult> {

    private final StorePlaylistsCommand storePlaylistsCommand;

    @Inject
    StoreRecommendedPlaylistsCommand(PropellerDatabase database, StorePlaylistsCommand storePlaylistsCommand) {
        super(database);
        this.storePlaylistsCommand = storePlaylistsCommand;
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final ModelCollection<ApiRecommendedPlaylistBucket> input) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(clearRecommendedPlaylistBucketTables(propeller));
                step(clearRecommendedPlaylistTables(propeller));
                for (ApiRecommendedPlaylistBucket playlistBucket : input) {
                    storePlaylistBucket(propeller, playlistBucket, input.getQueryUrn());
                }
            }

            private void storePlaylistBucket(PropellerDatabase propeller,
                                             ApiRecommendedPlaylistBucket playlistBucket,
                                             Optional<Urn> queryUrn) {
                final ContentValues values = buildRecommendedPlaylistBucketContentValues(playlistBucket, queryUrn);

                final InsertResult chartInsert = propeller.insert(Tables.RecommendedPlaylistBucket.TABLE, values);
                step(chartInsert);

                final long bucketId = chartInsert.getRowId();
                storePlaylistsCommand.call(playlistBucket.playlists());

                final List<ContentValues> playlists = new ArrayList<>(playlistBucket.playlists().size());
                for (ApiPlaylist apiPlaylist : playlistBucket.playlists()) {
                    playlists.add(buildRecommendedPlaylistContentValues(bucketId, apiPlaylist.getId()));
                }
                step(propeller.bulkInsert_experimental(Tables.RecommendedPlaylist.TABLE,
                                                       getRecommendedPlaylistColumns(),
                                                       playlists));
            }

        });
    }

    private WriteResult clearRecommendedPlaylistTables(PropellerDatabase propeller) {
        return propeller.delete(Tables.RecommendedPlaylist.TABLE);
    }

    private WriteResult clearRecommendedPlaylistBucketTables(PropellerDatabase propeller) {
        return propeller.delete(Tables.RecommendedPlaylistBucket.TABLE);
    }

    private Map<String, Class> getRecommendedPlaylistColumns() {
        final HashMap<String, Class> columns = new HashMap<>(2);
        columns.put(Tables.RecommendedPlaylist.BUCKET_ID.name(), Long.class);
        columns.put(Tables.RecommendedPlaylist.PLAYLIST_ID.name(), Long.class);
        return columns;
    }

    private ContentValues buildRecommendedPlaylistBucketContentValues(ApiRecommendedPlaylistBucket playlistBucket, Optional<Urn> queryUrn) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(Tables.RecommendedPlaylistBucket.KEY.name(), playlistBucket.key());
        contentValues.put(Tables.RecommendedPlaylistBucket.DISPLAY_NAME.name(), playlistBucket.displayName());
        contentValues.put(Tables.RecommendedPlaylistBucket.ARTWORK_URL.name(), playlistBucket.artworkUrl().orNull());
        contentValues.put(Tables.RecommendedPlaylistBucket.QUERY_URN.name(), queryUrn.isPresent() ? queryUrn.get().toString() : null);
        return contentValues;
}

    private ContentValues buildRecommendedPlaylistContentValues(long bucketId, long playlistId) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(Tables.RecommendedPlaylist.BUCKET_ID.name(), bucketId);
        contentValues.put(Tables.RecommendedPlaylist.PLAYLIST_ID.name(), playlistId);
        return contentValues;
    }
}
