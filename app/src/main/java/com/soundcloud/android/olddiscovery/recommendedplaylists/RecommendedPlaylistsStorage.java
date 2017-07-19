package com.soundcloud.android.olddiscovery.recommendedplaylists;

import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.ListMultiMap;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.MultiMap;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRxV2;
import com.soundcloud.propeller.rx.RxResultMapperV2;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;

import android.util.Pair;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecommendedPlaylistsStorage {

    private final Function<List<Pair<RecommendedPlaylistsEntity, Urn>>, List<RecommendedPlaylistsEntity>> toBucketWithPlaylistUrns = bucketsWithPlaylistUrn -> {
        final MultiMap<RecommendedPlaylistsEntity, Urn> bucketToUrnMap = new ListMultiMap<>();
        for (final Pair<RecommendedPlaylistsEntity, Urn> bucket : bucketsWithPlaylistUrn) {
            bucketToUrnMap.put(bucket.first, bucket.second);
        }
        final List<RecommendedPlaylistsEntity> result = new ArrayList<>(bucketToUrnMap.keySet().size());
        for (final RecommendedPlaylistsEntity entity : bucketToUrnMap.keySet()) {
            result.add(entity.copyWithPlaylistUrns(Lists.newArrayList(bucketToUrnMap.get(entity))));
        }
        Collections.sort(result, (lhs, rhs) -> Long.valueOf(lhs.getLocalId()).compareTo(Long.valueOf(rhs.getLocalId())));
        return result;
    };

    private static final RecommendedPlaylistEntityMapper RECOMMENDED_PLAYLIST_ENTITY_MAPPER = new RecommendedPlaylistEntityMapper();

    private final PropellerRxV2 propellerRx;
    private final Scheduler scheduler;

    @Inject
    RecommendedPlaylistsStorage(PropellerRxV2 propellerRx,
                                @Named(RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.propellerRx = propellerRx;
        this.scheduler = scheduler;
    }

    public void clear() {
        propellerRx.delete(Tables.RecommendedPlaylistBucket.TABLE).subscribe(new DefaultObserver<>());
        propellerRx.delete(Tables.RecommendedPlaylist.TABLE).subscribe(new DefaultObserver<>());
    }

    Maybe<List<RecommendedPlaylistsEntity>> recommendedPlaylists() {
        final Query query = Query.from(Tables.RecommendedPlaylistBucket.TABLE)
                                 .select(Tables.RecommendedPlaylistBucket._ID,
                                         Tables.RecommendedPlaylistBucket.KEY,
                                         Tables.RecommendedPlaylistBucket.DISPLAY_NAME,
                                         Tables.RecommendedPlaylistBucket.ARTWORK_URL,
                                         Tables.RecommendedPlaylistBucket.QUERY_URN,
                                         Tables.RecommendedPlaylist.PLAYLIST_ID)
                                 .leftJoin(Tables.RecommendedPlaylist.TABLE, Tables.RecommendedPlaylistBucket._ID, Tables.RecommendedPlaylist.BUCKET_ID)
                                 .order(Tables.RecommendedPlaylist._ID, Query.Order.ASC);

        return propellerRx.queryResult(query)
                          .subscribeOn(scheduler)
                          .map(result -> result.toList(RECOMMENDED_PLAYLIST_ENTITY_MAPPER))
                          .map(toBucketWithPlaylistUrns)
                          .firstElement();
    }

    private static final class RecommendedPlaylistEntityMapper extends RxResultMapperV2<Pair<RecommendedPlaylistsEntity, Urn>> {
        @Override
        public Pair<RecommendedPlaylistsEntity, Urn> map(CursorReader cursorReader) {
            Optional<Urn> queryUrn = Optional.absent();
            String queryUrnString = cursorReader.getString(Tables.RecommendedPlaylistBucket.QUERY_URN);

            if (queryUrnString != null) {
                queryUrn = Optional.of(new Urn(queryUrnString));
            }

            final RecommendedPlaylistsEntity playlist = RecommendedPlaylistsEntity.create(
                    cursorReader.getLong(Tables.RecommendedPlaylistBucket._ID),
                    cursorReader.getString(Tables.RecommendedPlaylistBucket.KEY),
                    cursorReader.getString(Tables.RecommendedPlaylistBucket.DISPLAY_NAME),
                    Optional.fromNullable(cursorReader.getString(Tables.RecommendedPlaylistBucket.ARTWORK_URL)),
                    queryUrn);
            final Urn playlistUrn = Urn.forPlaylist(cursorReader.getLong(Tables.RecommendedPlaylist.PLAYLIST_ID));
            return new Pair<>(playlist, playlistUrn);
        }
    }
}
