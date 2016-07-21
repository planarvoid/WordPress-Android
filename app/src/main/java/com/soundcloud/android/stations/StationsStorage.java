package com.soundcloud.android.stations;

import static com.soundcloud.propeller.ContentValuesBuilder.values;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.apply;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.Stations;
import com.soundcloud.android.storage.Tables.StationsCollections;
import com.soundcloud.android.storage.Tables.StationsPlayQueues;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import android.content.ContentValues;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

class StationsStorage {
    private static final String ONBOARDING_DISABLED = "ONBOARDING_DISABLED";
    private static final long EXPIRE_DELAY = TimeUnit.HOURS.toMillis(24);

    private static final Func1<CursorReader, Station> TO_STATION_WITHOUT_TRACKS = new Func1<CursorReader, Station>() {
        @Override
        public Station call(CursorReader cursorReader) {
            return new Station(
                    new Urn(cursorReader.getString(Stations.STATION_URN)),
                    cursorReader.getString(Stations.TITLE),
                    cursorReader.getString(Stations.TYPE),
                    Collections.<StationTrack>emptyList(),
                    cursorReader.getString(Stations.PERMALINK),
                    cursorReader.isNull(Stations.LAST_PLAYED_TRACK_POSITION)
                    ? com.soundcloud.android.stations.Stations.NEVER_PLAYED
                    : cursorReader.getInt(Stations.LAST_PLAYED_TRACK_POSITION),
                    Optional.fromNullable(cursorReader.getString(Stations.ARTWORK_URL_TEMPLATE)));
        }
    };

    private static final Func1<CursorReader, StationTrack> TO_STATION_TRACK = new Func1<CursorReader, StationTrack>() {
        @Override
        public StationTrack call(CursorReader cursorReader) {
            return StationTrack.create(
                    Urn.forTrack(cursorReader.getLong(StationsPlayQueues.TRACK_ID)),
                    new Urn(cursorReader.getString(StationsPlayQueues.QUERY_URN))
            );
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

    private final Func1<CursorReader, Observable<StationRecord>> toStation = new Func1<CursorReader, Observable<StationRecord>>() {
        @Override
        public Observable<StationRecord> call(CursorReader cursorReader) {
            return station(new Urn(cursorReader.getString(Stations.STATION_URN)));
        }
    };

    private final SharedPreferences sharedPreferences;
    private final PropellerDatabase propellerDatabase;
    private final PropellerRx propellerRx;
    private final DateProvider dateProvider;

    @Inject
    public StationsStorage(@Named(StorageModule.STATIONS) SharedPreferences sharedPreferences,
                           PropellerDatabase propellerDatabase,
                           CurrentDateProvider dateProvider) {
        this.sharedPreferences = sharedPreferences;
        this.propellerDatabase = propellerDatabase;
        this.propellerRx = new PropellerRx(propellerDatabase);
        this.dateProvider = dateProvider;
    }

    Observable<StationInfoTrack> stationTracks(Urn stationUrn) {
        final Query query = Query.from(Table.SoundView.name())
                                 .innerJoin(StationsPlayQueues.TABLE.name(),
                                            TableColumns.SoundView._ID,
                                            StationsPlayQueues.TRACK_ID.name())
                                 .select(TableColumns.SoundView._ID,
                                         TableColumns.SoundView.TITLE,
                                         TableColumns.SoundView.USERNAME,
                                         TableColumns.SoundView.USER_ID,
                                         TableColumns.SoundView.ARTWORK_URL)
                                 .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_TRACK)
                                 .whereEq(StationsPlayQueues.STATION_URN, stationUrn.toString())
                                 .order(StationsPlayQueues.POSITION, Query.Order.ASC);
        return propellerRx.query(query).map(new StationTrackMapper());
    }

    Observable<StationTrack> loadPlayQueue(Urn station, int startPosition) {
        return propellerRx
                .query(Query.from(StationsPlayQueues.TABLE)
                            .whereEq(StationsPlayQueues.STATION_URN, station.toString())
                            .whereGe(StationsPlayQueues.POSITION, startPosition)
                            .order(StationsPlayQueues.POSITION, Query.Order.ASC))
                .map(TO_STATION_TRACK);
    }

    Observable<TxnResult> clearExpiredPlayQueue(final Urn stationUrn) {
        return propellerRx.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                final Query isPlayQueueExpired =
                        apply(exists(Query.from(Stations.TABLE)
                                          .whereEq(Stations.STATION_URN,
                                                   stationUrn.toString())
                                          .whereLe(Stations.PLAY_QUEUE_UPDATED_AT,
                                                   dateProvider.getCurrentTime() - EXPIRE_DELAY)));

                if (propeller.query(isPlayQueueExpired).first(Boolean.class)) {

                    step(propeller.delete(StationsPlayQueues.TABLE, filter()
                            .whereEq(StationsPlayQueues.STATION_URN, stationUrn.toString())));
                    step(resetLastPlayedTrackPosition(stationUrn));
                }
            }
        });
    }

    void clear() {
        propellerDatabase.delete(Stations.TABLE);
        propellerDatabase.delete(StationsCollections.TABLE);
        propellerDatabase.delete(StationsPlayQueues.TABLE);
        sharedPreferences.edit().clear().apply();
    }

    Observable<StationRecord> getStationsCollection(int type) {
        return propellerRx
                .query(buildStationsQuery(type))
                .flatMap(toStation);
    }

    private Query buildStationsQuery(int collectionType) {
        return Query.from(StationsCollections.TABLE)
                    .whereEq(StationsCollections.COLLECTION_TYPE, collectionType)
                    .order(StationsCollections.UPDATED_LOCALLY_AT, Query.Order.DESC)
                    .order(StationsCollections.POSITION, Query.Order.ASC);
    }

    Observable<StationRecord> station(Urn stationUrn) {
        return Observable.zip(
                propellerRx.query(Query.from(Stations.TABLE)
                                       .whereEq(Stations.STATION_URN, stationUrn))
                           .map(TO_STATION_WITHOUT_TRACKS),

                propellerRx.query(buildTracksListQuery(stationUrn)).map(TO_STATION_TRACK).toList(),
                new Func2<Station, List<StationTrack>, StationRecord>() {
                    @Override
                    public StationRecord call(Station station, List<StationTrack> tracks) {
                        return new Station(
                                station.getUrn(),
                                station.getTitle(),
                                station.getType(),
                                tracks,
                                station.getPermalink(),
                                station.getPreviousPosition(),
                                station.getImageUrlTemplate());
                    }
                }
        );
    }

    private Query buildTracksListQuery(Urn stationUrn) {
        return Query.from(StationsPlayQueues.TABLE)
                    .select(StationsPlayQueues.TRACK_ID, StationsPlayQueues.QUERY_URN)
                    .whereEq(StationsPlayQueues.STATION_URN, stationUrn)
                    .order(StationsPlayQueues.POSITION, Query.Order.ASC);
    }

    ChangeResult saveLastPlayedTrackPosition(Urn stationUrn, int position) {
        return updateStation(stationUrn, values().put(Stations.LAST_PLAYED_TRACK_POSITION, position).get());
    }

    private ChangeResult resetLastPlayedTrackPosition(Urn stationUrn) {
        return updateStation(stationUrn, values().put(Stations.LAST_PLAYED_TRACK_POSITION, null).get());
    }

    private ChangeResult updateStation(Urn stationUrn, ContentValues contentValues) {
        return propellerDatabase.update(
                Stations.TABLE,
                contentValues,
                filter().whereEq(Stations.STATION_URN, stationUrn.toString())
        );
    }

    ChangeResult saveUnsyncedRecentlyPlayedStation(Urn stationUrn) {
        return propellerDatabase.upsert(
                StationsCollections.TABLE,
                values().put(StationsCollections.STATION_URN, stationUrn.toString())
                        .put(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.RECENT)
                        .put(StationsCollections.UPDATED_LOCALLY_AT, dateProvider.getCurrentTime())
                        .get()
        );
    }

    List<PropertySet> getRecentStationsToSync() {
        return propellerDatabase
                .query(Query.from(StationsCollections.TABLE)
                            .whereEq(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.RECENT)
                            .whereNotNull(StationsCollections.UPDATED_LOCALLY_AT))
                .toList(TO_RECENT_STATION);
    }

    boolean isOnboardingDisabled() {
        return sharedPreferences.getBoolean(ONBOARDING_DISABLED, false);
    }

    void disableOnboarding() {
        sharedPreferences.edit().putBoolean(ONBOARDING_DISABLED, true).apply();
    }

    private final class StationTrackMapper extends RxResultMapper<StationInfoTrack> {

        @Override
        public StationInfoTrack map(CursorReader reader) {
            return new StationInfoTrack(
                    Urn.forTrack(reader.getLong(TableColumns.SoundView._ID)),
                    reader.getString(TableColumns.SoundView.TITLE),
                    reader.getString(TableColumns.SoundView.USERNAME),
                    Urn.forUser(reader.getLong(TableColumns.SoundView.USER_ID)),
                    Optional.fromNullable(reader.getString(TableColumns.SoundView.ARTWORK_URL))
            );
        }
    }

}
