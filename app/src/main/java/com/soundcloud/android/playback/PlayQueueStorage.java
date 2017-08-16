package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlayQueueModel.CONTEXT_QUERY;
import static com.soundcloud.android.playback.PlayQueueModel.CONTEXT_TYPE;
import static com.soundcloud.android.playback.PlayQueueModel.CONTEXT_URN;
import static com.soundcloud.android.playback.PlayQueueModel.ENTITY_ID;
import static com.soundcloud.android.playback.PlayQueueModel.ENTITY_TYPE;
import static com.soundcloud.android.playback.PlayQueueModel.PLAYED;
import static com.soundcloud.android.playback.PlayQueueModel.QUERY_URN;
import static com.soundcloud.android.playback.PlayQueueModel.RELATED_ENTITY;
import static com.soundcloud.android.playback.PlayQueueModel.REPOSTER_ID;
import static com.soundcloud.android.playback.PlayQueueModel.SOURCE;
import static com.soundcloud.android.playback.PlayQueueModel.SOURCE_URN;
import static com.soundcloud.android.playback.PlayQueueModel.SOURCE_VERSION;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackContext.Bucket;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.optional.Optional;
import com.squareup.sqldelight.RowMapper;
import io.reactivex.Completable;
import io.reactivex.Single;

import android.database.Cursor;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueStorage {

    private static final String PLAY_QUEUE_TABLE = PlayQueueModel.TABLE_NAME;
    private static final int ENTITY_TYPE_TRACK = 0;
    private static final int ENTITY_TYPE_PLAYLIST = 1;

    private final PlayQueueDatabase playQueueDatabase;

    @Inject
    public PlayQueueStorage(PlayQueueDatabase playQueueDatabase) {
        this.playQueueDatabase = playQueueDatabase;
    }

    public List<Urn> getContextUrns() {
        return playQueueDatabase.executeQuery(DbModel.PlayQueue.FACTORY.selectAllContextUrns(), cursor -> new Urn(cursor.getString(0)));
    }

    Completable clear() {
        return Completable.fromAction(() -> playQueueDatabase.clear(PLAY_QUEUE_TABLE));
    }

    public void store(final PlayQueue playQueue) {
        playQueueDatabase.clear(PLAY_QUEUE_TABLE);

        playQueueDatabase.runInTransaction(() -> {
            PlayQueueModel.InsertRow insertRow = new PlayQueueModel.InsertRow(playQueueDatabase.writableDatabase());

            for (PlayQueueItem item : playQueue) {
                if (item.isPlayable()) {
                    bind(insertRow, (PlayableQueueItem) item);
                    playQueueDatabase.insert(PLAY_QUEUE_TABLE, insertRow.program);
                }
            }
        });
    }

    private void bind(PlayQueueModel.InsertRow insertRow, PlayableQueueItem playableQueueItem) {
        PlaybackContext playbackContext = playableQueueItem.getPlaybackContext();
        insertRow.bind(
                playableQueueItem.getUrn().getNumericId(),
                playableQueueItem.getUrn().isTrack() ? (long) ENTITY_TYPE_TRACK : ENTITY_TYPE_PLAYLIST,
                playableQueueItem.getReposter().isUser() ? playableQueueItem.getReposter().getNumericId() : Consts.NOT_SET,
                playableQueueItem.getRelatedEntity().equals(Urn.NOT_SET) ? null : playableQueueItem.getRelatedEntity().toString(),
                playableQueueItem.getSource(),
                playableQueueItem.getSourceVersion(),
                playableQueueItem.getSourceUrn().equals(Urn.NOT_SET) ? null : playableQueueItem.getSourceUrn().toString(),
                playableQueueItem.getQueryUrn().equals(Urn.NOT_SET) ? null : playableQueueItem.getQueryUrn().toString(),
                playbackContext.bucket().toString(),
                playbackContext.urn().isPresent() ? playbackContext.urn().get().toString() : null,
                playbackContext.query().isPresent() ? playbackContext.query().get() : null,
                playableQueueItem.isPlayed()
        );
    }

    Single<List<PlayQueueItem>> load() {
        return playQueueDatabase.executeAsyncQuery(DbModel.PlayQueue.FACTORY.selectAll(), cursor -> new PlayableQueueItemMapper().map(cursor));
    }

    Single<List<PlayableQueueItem>> loadPlayableQueueItems() {
        return playQueueDatabase.executeAsyncQuery(DbModel.PlayQueue.FACTORY.selectAll(), new PlayableQueueItemMapper());
    }

    private class PlayableQueueItemMapper implements RowMapper<PlayableQueueItem> {

        @NonNull
        @Override
        public PlayableQueueItem map(@NonNull Cursor cursor) {

            final Urn relatedEntityUrn = getRelatedEntityUrn(cursor);
            final Urn reposterUrn = getReposterUrn(cursor);
            final Urn queryUrn = getQueryUrn(cursor);
            final Urn sourceUrn = getSourceUrn(cursor);
            final String source = cursor.getString(cursor.getColumnIndex(SOURCE));
            final String sourceVersion = cursor.getString(cursor.getColumnIndex(SOURCE_VERSION));
            final boolean played = cursor.getInt(cursor.getColumnIndex(PLAYED)) != 0;
            final double entityType = cursor.getInt(cursor.getColumnIndex(ENTITY_TYPE));
            final long entityId = cursor.getLong(cursor.getColumnIndex(ENTITY_ID));

            final PlaybackContext playbackContext = PlaybackContext.builder()
                                                                   .bucket(getPlaybackContextBucket(cursor))
                                                                   .urn(getPlaybackContextUrn(cursor))
                                                                   .query(getPlaybackContextQuery(cursor))
                                                                   .build();

            if (entityType == ENTITY_TYPE_PLAYLIST) {
                final Urn playlist = Urn.forPlaylist(entityId);
                return new PlaylistQueueItem.Builder(playlist)
                        .relatedEntity(relatedEntityUrn)
                        .fromSource(source, sourceVersion, sourceUrn, queryUrn)
                        .withPlaybackContext(playbackContext)
                        .played(played)
                        .build();
            } else {
                final Urn track = Urn.forTrack(entityId);
                return new TrackQueueItem.Builder(track, reposterUrn)
                        .relatedEntity(relatedEntityUrn)
                        .withPlaybackContext(playbackContext)
                        .fromSource(source, sourceVersion, sourceUrn, queryUrn)
                        .played(played)
                        .build();
            }
        }

        private Urn getSourceUrn(@NonNull Cursor cursor) {
            int columnIndex = cursor.getColumnIndex(SOURCE_URN);
            String result = cursor.getString(columnIndex);
            return result == null ? Urn.NOT_SET : new Urn(result);
        }

        private Urn getQueryUrn(@NonNull Cursor cursor) {
            int columnIndex = cursor.getColumnIndex(QUERY_URN);
            String result = cursor.getString(columnIndex);
            return result == null ? Urn.NOT_SET : new Urn(result);
        }

        private Urn getReposterUrn(@NonNull Cursor cursor) {
            int columnIndex = cursor.getColumnIndex(REPOSTER_ID);
            int result = cursor.getInt(columnIndex);
            return result > 0 ? Urn.forUser(result) : Urn.NOT_SET;
        }

        private Urn getRelatedEntityUrn(@NonNull Cursor cursor) {
            int columnIndex = cursor.getColumnIndex(RELATED_ENTITY);
            String result = cursor.getString(columnIndex);
            return result == null ? Urn.NOT_SET : new Urn(result);
        }

        private Bucket getPlaybackContextBucket(Cursor cursor) {
            int columnIndex = cursor.getColumnIndex(CONTEXT_TYPE);
            String result = cursor.getString(columnIndex);
            return result == null ? Bucket.OTHER : Bucket.valueOf(result);
        }

        private Optional<String> getPlaybackContextQuery(Cursor cursor) {
            int columnIndex = cursor.getColumnIndex(CONTEXT_QUERY);
            String result = cursor.getString(columnIndex);
            return result == null ? Optional.absent() : Optional.of(result);
        }

        private Optional<Urn> getPlaybackContextUrn(Cursor cursor) {
            int columnIndex = cursor.getColumnIndex(CONTEXT_URN);
            String result = cursor.getString(columnIndex);
            return result == null ? Optional.absent() : Optional.of(new Urn(result));
        }

    }
}
