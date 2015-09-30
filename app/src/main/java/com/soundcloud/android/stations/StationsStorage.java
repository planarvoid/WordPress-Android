package com.soundcloud.android.stations;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.Stations;
import com.soundcloud.android.storage.Tables.StationsCollections;
import com.soundcloud.android.storage.Tables.StationsPlayQueues;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

class StationsStorage {
    private static final Func1<CursorReader, Station> TO_STATION_WITHOUT_TRACKS = new Func1<CursorReader, Station>() {
        @Override
        public Station call(CursorReader cursorReader) {
            return new Station(
                    new Urn(cursorReader.getString(Stations.STATION_URN)),
                    cursorReader.getString(Stations.TITLE),
                    cursorReader.getString(Stations.TYPE),
                    Collections.<Urn>emptyList(),
                    cursorReader.getString(Stations.PERMALINK),
                    cursorReader.isNull(Stations.LAST_PLAYED_TRACK_POSITION)
                            ? com.soundcloud.android.stations.Stations.NEVER_PLAYED
                            : cursorReader.getInt(Stations.LAST_PLAYED_TRACK_POSITION)
            );
        }
    };

    private static final Func1<CursorReader, Urn> TO_TRACK_URN = new Func1<CursorReader, Urn>() {
        @Override
        public Urn call(CursorReader cursorReader) {
            return new Urn(cursorReader.getString(StationsPlayQueues.TRACK_URN));
        }
    };

    private static final ResultMapper<PropertySet> TO_RECENT_STATION = new ResultMapper<PropertySet>() {
        @Override
        public PropertySet map(CursorReader reader) {
            return PropertySet.from(
                    StationProperty.URN.bind(new Urn(reader.getString(StationsCollections.STATION_URN))),
                    StationProperty.UPDATED_LOCALLY_AT.bind(reader.getLong(StationsCollections.UPDATED_LOCALLY_AT)),
                    StationProperty.POSITION.bind(reader.getInt(StationsCollections.POSITION))
            );
        }
    };

    private final Func1<CursorReader, Observable<Station>> toStation = new Func1<CursorReader, Observable<Station>>() {
        @Override
        public Observable<Station> call(CursorReader cursorReader) {
            return station(new Urn(cursorReader.getString(Stations.STATION_URN)));
        }
    };

    private final PropellerDatabase propellerDatabase;
    private final PropellerRx propellerRx;
    private final DateProvider dateProvider;

    @Inject
    public StationsStorage(PropellerDatabase propellerDatabase, PropellerRx propellerRx, CurrentDateProvider dateProvider) {
        this.propellerDatabase = propellerDatabase;
        this.propellerRx = propellerRx;
        this.dateProvider = dateProvider;
    }

    public Observable<Urn> loadPlayQueue(Urn station, int startPosition) {
        return propellerRx
                .query(Query
                        .from(StationsPlayQueues.TABLE)
                        .whereEq(StationsPlayQueues.STATION_URN, station.toString())
                        .whereGe(StationsPlayQueues.POSITION, startPosition)
                        .order(StationsPlayQueues.POSITION, Query.Order.ASC))
                .map(TO_TRACK_URN);
    }

    void clear() {
        propellerDatabase.delete(Stations.TABLE);
        propellerDatabase.delete(StationsCollections.TABLE);
        propellerDatabase.delete(StationsPlayQueues.TABLE);
    }

    Observable<Station> getStationsCollection(int type) {
        return propellerRx
                .query(buildStationsQuery(type))
                .flatMap(toStation);
    }

    private Query buildStationsQuery(int collectionType) {
        return Query
                .from(StationsCollections.TABLE)
                .whereEq(StationsCollections.COLLECTION_TYPE, collectionType)
                .order(StationsCollections.UPDATED_LOCALLY_AT, Query.Order.DESC)
                .order(StationsCollections.POSITION, Query.Order.ASC);
    }

    Observable<Station> station(Urn stationUrn) {
        return Observable.zip(
                propellerRx.query(Query.from(Stations.TABLE).whereEq(Stations.STATION_URN, stationUrn)).map(TO_STATION_WITHOUT_TRACKS),
                propellerRx.query(buildTracksListQuery(stationUrn)).map(TO_TRACK_URN).toList(),
                new Func2<Station, List<Urn>, Station>() {
                    @Override
                    public Station call(Station station, List<Urn> tracks) {
                        return new Station(
                                station.getUrn(),
                                station.getTitle(),
                                station.getType(),
                                tracks,
                                station.getPermalink(),
                                station.getPreviousPosition()
                        );
                    }
                }
        );
    }

    private Query buildTracksListQuery(Urn stationUrn) {
        return Query
                .from(StationsPlayQueues.TABLE)
                .select(StationsPlayQueues.TRACK_URN)
                .whereEq(StationsPlayQueues.STATION_URN, stationUrn)
                .order(StationsPlayQueues.POSITION, Query.Order.ASC);
    }

    Observable<ChangeResult> saveLastPlayedTrackPosition(Urn stationUrn, int position) {
        return propellerRx.update(
                Stations.TABLE,
                ContentValuesBuilder.values().put(Stations.LAST_PLAYED_TRACK_POSITION, position).get(),
                filter().whereEq(Stations.STATION_URN, stationUrn.toString())
        );
    }

    Observable<ChangeResult> saveUnsyncedRecentlyPlayedStation(Urn stationUrn) {
        return propellerRx.upsert(
                StationsCollections.TABLE,
                ContentValuesBuilder
                        .values()
                        .put(StationsCollections.STATION_URN, stationUrn.toString())
                        .put(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.RECENT)
                        .put(StationsCollections.UPDATED_LOCALLY_AT, dateProvider.getCurrentTime())
                        .get()
        );
    }

    List<PropertySet> getRecentStationsToSync() {
        return propellerDatabase
                .query(Query
                        .from(StationsCollections.TABLE)
                        .whereEq(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.RECENT)
                        .whereNotNull(StationsCollections.UPDATED_LOCALLY_AT))
                .toList(TO_RECENT_STATION);
    }
}
