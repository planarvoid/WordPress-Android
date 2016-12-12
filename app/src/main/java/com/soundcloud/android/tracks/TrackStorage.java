package com.soundcloud.android.tracks;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.TableColumns.ResourceTable._TYPE;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;
import static com.soundcloud.java.collections.Lists.partition;
import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;
import rx.functions.Func1;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TrackStorage {

    private static final int MAX_TRACKS_BATCH = 200;

    private final PropellerRx propeller;
    private final TrackItemMapper trackMapper = new TrackItemMapper();

    private Func1<List<Urn>, Observable<QueryResult>> fetchTracks = new Func1<List<Urn>, Observable<QueryResult>>() {
        @Override
        public Observable<QueryResult> call(List<Urn> urns) {
            return propeller.queryResult(buildTracksQuery(urns));
        }
    };

    private Func1<List<Urn>, Observable<Urn>> fetchAvailableTrackUrns = new Func1<List<Urn>, Observable<Urn>>() {
        @Override
        public Observable<Urn> call(List<Urn> urns) {
            return propeller.query(buildAvailableTracksQuery(urns))
                            .map(new TrackUrnMapper());
        }
    };

    @Inject
    TrackStorage(PropellerRx propeller) {
        this.propeller = propeller;
    }

    Observable<PropertySet> loadTrack(Urn urn) {
        return propeller.query(buildTrackQuery(urn))
                        .map(trackMapper)
                        .firstOrDefault(PropertySet.create());
    }

    Observable<Map<Urn, TrackItem>> loadTracks(List<Urn> urns) {
        return batchedTracks(urns).toList()
                                  .map(this::toMapOfUrnAndTrack)
                                  .firstOrDefault(Collections.<Urn, TrackItem>emptyMap());
    }

    private Observable<QueryResult> batchedTracks(List<Urn> urns) {
        return Observable.from(partition(urns, MAX_TRACKS_BATCH))
                         .flatMap(fetchTracks);
    }

    private Observable<Urn> batchedAvailableTracks(List<Urn> urns) {
        return Observable.from(partition(urns, MAX_TRACKS_BATCH)).flatMap(fetchAvailableTrackUrns);
    }

    private Map<Urn, TrackItem> toMapOfUrnAndTrack(List<QueryResult> cursorReadersBatches) {
        final Map<Urn, TrackItem> tracks = new HashMap<>(cursorReadersBatches.size() * MAX_TRACKS_BATCH);
        for (QueryResult cursorReaders : cursorReadersBatches) {
            for (CursorReader cursorReader : cursorReaders) {
                final PropertySet track = trackMapper.map(cursorReader);
                final Urn urn = track.get(TrackProperty.URN);
                tracks.put(urn, TrackItem.from(track));
            }
        }
        return tracks;
    }

    Observable<List<Urn>> availableTracks(final List<Urn> requestedTracks) {
        return batchedAvailableTracks(requestedTracks)
                .toList()
                .firstOrDefault(Collections.emptyList());
    }

    Observable<PropertySet> loadTrackDescription(Urn urn) {
        return propeller.query(buildTrackDescriptionQuery(urn))
                        .map(new TrackDescriptionMapper())
                        .firstOrDefault(PropertySet.create());
    }

    private Query buildTrackDescriptionQuery(Urn trackUrn) {
        return Query.from(Table.SoundView.name())
                    .select(TableColumns.SoundView.DESCRIPTION)
                    .whereEq(_ID, trackUrn.getNumericId())
                    .whereEq(_TYPE, TYPE_TRACK);
    }

    private Query buildTrackQuery(Urn trackUrn) {
        return Query.from(Tables.TrackView.TABLE)
                    .select("*")
                    .whereEq(Tables.TrackView.ID.name(), trackUrn.getNumericId());
    }

    private Query buildTracksQuery(List<Urn> trackUrns) {
        return Query.from(Tables.TrackView.TABLE)
                    .select("*")
                    .whereIn(Tables.TrackView.ID.name(), transform(trackUrns, Urns.TO_ID));
    }

    private Query buildAvailableTracksQuery(List<Urn> trackUrns) {
        return Query.from(Tables.Sounds.TABLE)
                    .select(Tables.Sounds._ID.as(BaseColumns._ID))
                    .whereEq(Tables.Sounds._TYPE, TYPE_TRACK)
                    .whereIn(Tables.Sounds._ID, transform(trackUrns, Urns.TO_ID));
    }

}
