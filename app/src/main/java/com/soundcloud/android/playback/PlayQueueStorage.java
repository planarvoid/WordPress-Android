package com.soundcloud.android.playback;

import static com.soundcloud.android.storage.Tables.PlayQueue.ENTITY_TYPE_PLAYLIST;
import static com.soundcloud.android.storage.Tables.PlayQueue.ENTITY_TYPE_TRACK;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import com.soundcloud.propeller.schema.Table;
import rx.Observable;
import rx.functions.Func1;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class PlayQueueStorage {

    private static final Table TABLE = Tables.PlayQueue.TABLE;

    private final PropellerRx propellerRx;

    @Inject
    public PlayQueueStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    public Observable<ChangeResult> clearAsync() {
        return propellerRx.truncate(TABLE);
    }

    public Observable<TxnResult> storeAsync(final PlayQueue playQueue) {
        final List<ContentValues> newItems = new ArrayList<>(playQueue.size());
        for (PlayQueueItem item : playQueue) {
            if (item.shouldPersist()) {
                if (item.isTrack() || item.isPlaylist()) {
                    newItems.add(entityItemContentValues((PlayableQueueItem) item));
                } else {
                    ErrorUtils.handleSilentException(new IllegalStateException(
                            "Tried to persist an unsupported play queue item"));
                }
            }
        }

        return clearAsync().flatMap(new Func1<ChangeResult, Observable<TxnResult>>() {
            @Override
            public Observable<TxnResult> call(ChangeResult changeResult) {
                return propellerRx.bulkInsert(TABLE, newItems);
            }
        });
    }

    public Observable<PlayQueueItem> loadAsync() {
        return propellerRx.query(Query.from(TABLE.name())).map(new RxResultMapper<PlayQueueItem>() {
            @Override
            public PlayQueueItem map(CursorReader reader) {
                final Urn relatedEntity = hasRelatedEntity(reader) ?
                                          new Urn(reader.getString(Tables.PlayQueue.RELATED_ENTITY)) :
                                          Urn.NOT_SET;
                final Urn reposter = hasReposter(reader) ?
                                     Urn.forUser(reader.getLong(Tables.PlayQueue.REPOSTER_ID)) :
                                     Urn.NOT_SET;
                final String source = reader.getString(Tables.PlayQueue.SOURCE);
                final String sourceVersion = reader.getString(Tables.PlayQueue.SOURCE_VERSION);
                final Urn sourceUrn = hasSourceUrn(reader) ?
                                      new Urn(reader.getString(Tables.PlayQueue.SOURCE_URN)) :
                                      Urn.NOT_SET;
                final Urn queryUrn = hasQueryUrn(reader) ?
                                     new Urn(reader.getString(Tables.PlayQueue.QUERY_URN)) :
                                     Urn.NOT_SET;

                if (reader.getInt(Tables.PlayQueue.ENTITY_TYPE.name()) == ENTITY_TYPE_PLAYLIST) {
                    final Urn playlist = Urn.forPlaylist(reader.getLong(Tables.PlayQueue.ENTITY_ID));
                    return new PlaylistQueueItem.Builder(playlist)
                            .relatedEntity(relatedEntity)
                            .fromSource(source, sourceVersion, sourceUrn, queryUrn)
                            // TODO: Context will come from storage in an upcoming PR
                            .withPlaybackContext(PlaybackContext.create(PlaySessionSource.EMPTY))
                            .build();
                } else {
                    final Urn track = Urn.forTrack(reader.getLong(Tables.PlayQueue.ENTITY_ID));
                    return new TrackQueueItem.Builder(track, reposter)
                            .relatedEntity(relatedEntity)
                            // TODO: Context will come from storage in an upcoming PR
                            .withPlaybackContext(PlaybackContext.create(PlaySessionSource.EMPTY))
                            .fromSource(source, sourceVersion, sourceUrn, queryUrn)
                            .build();
                }
            }
        });
    }

    private ContentValues entityItemContentValues(PlayableQueueItem playableQueueItem) {
        final ContentValuesBuilder valuesBuilder = ContentValuesBuilder.values()
                                                                       .put(Tables.PlayQueue.ENTITY_ID,
                                                                            playableQueueItem.getUrn().getNumericId())
                                                                       .put(Tables.PlayQueue.ENTITY_TYPE.name(),
                                                                            playableQueueItem.getUrn().isTrack() ?
                                                                            ENTITY_TYPE_TRACK :
                                                                            ENTITY_TYPE_PLAYLIST)
                                                                       .put(Tables.PlayQueue.SOURCE,
                                                                            playableQueueItem.getSource())
                                                                       .put(Tables.PlayQueue.SOURCE_VERSION,
                                                                            playableQueueItem.getSourceVersion());

        if (!playableQueueItem.getRelatedEntity().equals(Urn.NOT_SET)) {
            valuesBuilder.put(Tables.PlayQueue.RELATED_ENTITY, playableQueueItem.getRelatedEntity().toString());
        }

        if (playableQueueItem.getReposter().isUser()) {
            valuesBuilder.put(Tables.PlayQueue.REPOSTER_ID, playableQueueItem.getReposter().getNumericId());
        }

        if (!playableQueueItem.getSourceUrn().equals(Urn.NOT_SET)) {
            valuesBuilder.put(Tables.PlayQueue.SOURCE_URN, playableQueueItem.getSourceUrn().toString());
        }

        if (!playableQueueItem.getQueryUrn().equals(Urn.NOT_SET)) {
            valuesBuilder.put(Tables.PlayQueue.QUERY_URN, playableQueueItem.getQueryUrn().toString());
        }

        return valuesBuilder.get();
    }

    private boolean hasRelatedEntity(CursorReader reader) {
        return reader.isNotNull(Tables.PlayQueue.RELATED_ENTITY);
    }

    private boolean hasReposter(CursorReader reader) {
        return reader.isNotNull(Tables.PlayQueue.REPOSTER_ID) && reader.getLong(Tables.PlayQueue.REPOSTER_ID) > 0;
    }

    private boolean hasSourceUrn(CursorReader reader) {
        return reader.isNotNull(Tables.PlayQueue.SOURCE_URN);
    }

    private boolean hasQueryUrn(CursorReader reader) {
        return reader.isNotNull(Tables.PlayQueue.QUERY_URN);
    }
}