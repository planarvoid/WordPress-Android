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
import com.soundcloud.propeller.schema.BulkInsertValues;

import android.content.ContentValues;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.Arrays;

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

                BulkInsertValues bucketValues = getPlaylistBucketValues(playlistBucket, bucketId).build();
                step(propeller.bulkInsert(Tables.RecommendedPlaylist.TABLE, bucketValues));
            }

        });
    }

    @NonNull
    private BulkInsertValues.Builder getPlaylistBucketValues(ApiRecommendedPlaylistBucket playlistBucket,
                                                             long bucketId) {
        final BulkInsertValues.Builder builder = new BulkInsertValues.Builder(
                Arrays.asList(
                        Tables.RecommendedPlaylist.BUCKET_ID,
                        Tables.RecommendedPlaylist.PLAYLIST_ID
                )
        );
        for (ApiPlaylist playlist : playlistBucket.playlists()) {
            builder.addRow(Arrays.asList(bucketId, playlist.getId()));
        }
        return builder;
    }

    private WriteResult clearRecommendedPlaylistTables(PropellerDatabase propeller) {
        return propeller.delete(Tables.RecommendedPlaylist.TABLE);
    }

    private WriteResult clearRecommendedPlaylistBucketTables(PropellerDatabase propeller) {
        return propeller.delete(Tables.RecommendedPlaylistBucket.TABLE);
    }

    private ContentValues buildRecommendedPlaylistBucketContentValues(ApiRecommendedPlaylistBucket playlistBucket,
                                                                      Optional<Urn> queryUrn) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(Tables.RecommendedPlaylistBucket.KEY.name(), playlistBucket.key());
        contentValues.put(Tables.RecommendedPlaylistBucket.DISPLAY_NAME.name(), playlistBucket.displayName());
        contentValues.put(Tables.RecommendedPlaylistBucket.ARTWORK_URL.name(), playlistBucket.artworkUrl().orNull());
        contentValues.put(Tables.RecommendedPlaylistBucket.QUERY_URN.name(), queryUrn.isPresent() ? queryUrn.get().toString() : null);
        return contentValues;
    }
}
