package com.soundcloud.android.playback;

import static com.soundcloud.android.storage.Tables.PlayQueue.ENTITY_TYPE_PLAYLIST;
import static com.soundcloud.android.storage.Tables.PlayQueue.ENTITY_TYPE_TRACK;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackContext.Bucket;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.QueryResult;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayQueueStorage {

    private static final Table TABLE = Tables.PlayQueue.TABLE;

    private final PropellerRx propellerRx;

    private Func1<QueryResult, Map<Urn, String>> toMapOfUrnAndTitles = new Func1<QueryResult, Map<Urn, String>>() {
        @Override
        public Map<Urn, String> call(QueryResult cursorReaders) {
            return toMapOfUrnAndTitles(cursorReaders);
        }
    };

    @Inject
    public PlayQueueStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    public Observable<ChangeResult> clearAsync() {
        return propellerRx.truncate(TABLE);
    }

    Observable<TxnResult> storeAsync(final PlayQueue playQueue) {
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
                return propellerRx.bulkInsertExperimental(TABLE, getColumnTypes(), newItems);
            }
        });
    }

    private Map<String, Class> getColumnTypes() {
        final HashMap<String, Class> columns = new HashMap<>();
        columns.put(Tables.PlayQueue.ENTITY_ID.name(), Long.class);
        columns.put(Tables.PlayQueue.ENTITY_TYPE.name(), Integer.class);
        columns.put(Tables.PlayQueue.REPOSTER_ID.name(), Long.class);
        columns.put(Tables.PlayQueue.RELATED_ENTITY.name(), String.class);
        columns.put(Tables.PlayQueue.SOURCE.name(), String.class);
        columns.put(Tables.PlayQueue.SOURCE_VERSION.name(), String.class);
        columns.put(Tables.PlayQueue.SOURCE_URN.name(), String.class);
        columns.put(Tables.PlayQueue.QUERY_URN.name(), String.class);
        columns.put(Tables.PlayQueue.CONTEXT_TYPE.name(), String.class);
        columns.put(Tables.PlayQueue.CONTEXT_URN.name(), String.class);
        columns.put(Tables.PlayQueue.CONTEXT_QUERY.name(), String.class);
        return columns;
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

                // When the storage didn't have a playback context (i.e. database migrated),
                // we fallback to EXPLICIT context
                final Bucket playbackContextBucket = hasPlaybackContextType(reader) ?
                                                     Bucket.valueOf(reader.getString(Tables.PlayQueue.CONTEXT_TYPE)) :
                                                     Bucket.EXPLICIT;

                final Optional<Urn> playbackContextUrn = hasPlaybackContextUrn(reader) ?
                                                         Optional.of(new Urn(reader.getString(Tables.PlayQueue.CONTEXT_URN))) :
                                                         Optional.<Urn>absent();

                final Optional<String> playbackContextQuery = hasPlaybackContextQuery(reader) ?
                                                              Optional.of(reader.getString(Tables.PlayQueue.CONTEXT_QUERY)) :
                                                              Optional.<String>absent();

                final PlaybackContext playbackContext = PlaybackContext.builder()
                                                                       .bucket(playbackContextBucket)
                                                                       .urn(playbackContextUrn)
                                                                       .query(playbackContextQuery)
                                                                       .build();

                if (reader.getInt(Tables.PlayQueue.ENTITY_TYPE.name()) == ENTITY_TYPE_PLAYLIST) {
                    final Urn playlist = Urn.forPlaylist(reader.getLong(Tables.PlayQueue.ENTITY_ID));
                    return new PlaylistQueueItem.Builder(playlist)
                            .relatedEntity(relatedEntity)
                            .fromSource(source, sourceVersion, sourceUrn, queryUrn)
                            .withPlaybackContext(playbackContext)
                            .build();
                } else {
                    final Urn track = Urn.forTrack(reader.getLong(Tables.PlayQueue.ENTITY_ID));
                    return new TrackQueueItem.Builder(track, reposter)
                            .relatedEntity(relatedEntity)
                            // TODO: Context will come from storage in an upcoming PR
                            .withPlaybackContext(playbackContext)
                            .fromSource(source, sourceVersion, sourceUrn, queryUrn)
                            .build();
                }
            }
        });
    }

    public Observable<Map<Urn, String>> contextTitles() {
        return propellerRx.queryResult(loadContextsQuery())
                          .map(toMapOfUrnAndTitles);
    }

    private ContentValues entityItemContentValues(PlayableQueueItem playableQueueItem) {
        final PlaybackContext playbackContext = playableQueueItem.getPlaybackContext();
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
                                                                            playableQueueItem.getSourceVersion())
                                                                       .put(Tables.PlayQueue.CONTEXT_TYPE,
                                                                            playbackContext.bucket().toString());

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

        if (playbackContext.urn().isPresent()) {
            valuesBuilder.put(Tables.PlayQueue.CONTEXT_URN, playbackContext.urn().get().toString());
        }

        if (playbackContext.query().isPresent()) {
            valuesBuilder.put(Tables.PlayQueue.CONTEXT_QUERY, playbackContext.query().get());
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

    private boolean hasPlaybackContextType(CursorReader reader) {
        return reader.isNotNull(Tables.PlayQueue.CONTEXT_TYPE);
    }

    private boolean hasPlaybackContextUrn(CursorReader reader) {
        return reader.isNotNull(Tables.PlayQueue.CONTEXT_URN);
    }

    private boolean hasPlaybackContextQuery(CursorReader reader) {
        return reader.isNotNull(Tables.PlayQueue.CONTEXT_QUERY);
    }

    private String loadContextsQuery() {
        return "SELECT " +
                "    pq.context_urn as " + Tables.PlayQueue.CONTEXT_URN.name() + "," +
                "    coalesce(stations.title, playlistView.pv_title, users.username, charts.display_name) as title" +
                "  FROM " + Tables.PlayQueue.TABLE.name() + " as pq" +
                "  left join stations on stations.station_urn = pq.context_urn " +
                "  left join playlistView on 'soundcloud:playlists:' || playlistView.pv_id = pq.context_urn" +
                "  left join users on 'soundcloud:users:' || users._id = pq.context_urn" +
                "  left join charts on charts.genre = pq.context_urn" +
                "  WHERE pq.context_urn IS NOT NULL" +
                "  GROUP BY pq.context_urn";
    }

    private Map<Urn, String> toMapOfUrnAndTitles(QueryResult cursorReaders) {
        final Map<Urn, String> titles = new HashMap<>(cursorReaders.getResultCount());
        for (CursorReader cursorReader : cursorReaders) {
            final Urn context_urn = new Urn(cursorReader.getString("context_urn"));
            final String title = cursorReader.getString("title");
            titles.put(context_urn, title);
        }
        return titles;
    }


}
