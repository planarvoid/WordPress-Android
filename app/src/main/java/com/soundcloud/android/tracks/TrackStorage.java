package com.soundcloud.android.tracks;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.TableColumns.ResourceTable._TYPE;
import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.commands.TrackViewUrnMapper;
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

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TrackStorage {

    private final PropellerRx propeller;
    private final TrackItemMapper trackMapper = new TrackItemMapper();
    private Func1<QueryResult, Map<Urn, PropertySet>> toMapOfUrnAndPropertySet = new Func1<QueryResult, Map<Urn, PropertySet>>() {
        @Override
        public Map<Urn, PropertySet> call(QueryResult cursorReaders) {
            return toMapOfUrnAndPropertySet(cursorReaders);
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

    Observable<Map<Urn, PropertySet>> loadTracks(List<Urn> urns) {
        return propeller.queryResult(buildTracksQuery(urns))
                        .map(toMapOfUrnAndPropertySet)
                        .firstOrDefault(Collections.<Urn, PropertySet>emptyMap());
    }

    private Map<Urn, PropertySet> toMapOfUrnAndPropertySet(QueryResult cursorReaders) {
        final Map<Urn, PropertySet> tracks = new HashMap<>(cursorReaders.getResultCount());
        for (CursorReader cursorReader : cursorReaders) {
            final PropertySet track = trackMapper.map(cursorReader);
            final Urn urn = track.get(TrackProperty.URN);

            tracks.put(urn, track);
        }
        return tracks;
    }

    public Observable<List<Urn>> availableTracks(final List<Urn> requestedTracks) {
        return propeller
                .query(buildTracksQuery(requestedTracks))
                .map(new TrackViewUrnMapper())
                .toList();
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
                    .whereEq(_TYPE, TableColumns.Sounds.TYPE_TRACK);
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
}
