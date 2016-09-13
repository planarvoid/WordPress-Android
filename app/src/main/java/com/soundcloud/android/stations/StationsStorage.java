package com.soundcloud.android.stations;

import static com.soundcloud.android.model.PlayableProperty.CREATOR_NAME;
import static com.soundcloud.android.model.PlayableProperty.CREATOR_URN;
import static com.soundcloud.android.model.PlayableProperty.IMAGE_URL_TEMPLATE;
import static com.soundcloud.android.model.PlayableProperty.TITLE;
import static com.soundcloud.android.model.PlayableProperty.URN;
import static com.soundcloud.android.stations.Stations.NEVER_PLAYED;
import static com.soundcloud.android.tracks.TrackProperty.PLAY_COUNT;
import static com.soundcloud.java.collections.Lists.transform;
import static com.soundcloud.java.optional.Optional.fromNullable;
import static com.soundcloud.propeller.ContentValuesBuilder.values;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.apply;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.TableColumns.SoundView;
import com.soundcloud.android.storage.Tables.Stations;
import com.soundcloud.android.storage.Tables.StationsCollections;
import com.soundcloud.android.storage.Tables.StationsPlayQueues;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import com.soundcloud.propeller.schema.Column;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

class StationsStorage {

    private static final String STATION_LIKE = "STATION_LIKE";
    private static final String ONBOARDING_LIKED_STATIONS_DISABLED = "ONBOARDING_LIKED_STATIONS_DISABLED";
    private static final String ONBOARDING_STREAM_ITEM_DISABLED = "ONBOARDING_STREAM_ITEM_DISABLED";
    private static final String MIGRATE_RECENT_TO_LIKED_STATIONS = "MIGRATE_RECENT_TO_LIKED_STATIONS";
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
                    mapLastPosition(cursorReader),
                    fromNullable(cursorReader.getString(Stations.ARTWORK_URL_TEMPLATE)));
        }
    };

    private static final ResultMapper<Urn> STATIONS_COLLECTIONS_TO_URN = new ResultMapper<Urn>() {
        @Override
        public Urn map(CursorReader reader) {
            return new Urn(reader.getString(StationsCollections.STATION_URN));
        }
    };

    private static final ResultMapper<Urn> STATIONS_TO_URN = new ResultMapper<Urn>() {
        @Override
        public Urn map(CursorReader reader) {
            return new Urn(reader.getString(Stations.STATION_URN));
        }
    };

    private static final Func2<StationWithTracks, List<StationInfoTrack>, StationWithTracks> ZIP_STATION_WITH_TRACKS = new Func2<StationWithTracks, List<StationInfoTrack>, StationWithTracks>() {
        @Override
        public StationWithTracks call(StationWithTracks stationWithTracks,
                                      List<StationInfoTrack> stationInfoTracks) {
            stationWithTracks.setTracks(stationInfoTracks);
            return stationWithTracks;
        }
    };

    private static final Function<ApiStationMetadata, ContentValues> TO_STATION_METADATA = new Function<ApiStationMetadata, ContentValues>() {
        @Override
        public ContentValues apply(ApiStationMetadata station) {
            final ContentValuesBuilder contentValuesBuilder = values()
                    .put(Stations.STATION_URN, station.getUrn().toString())
                    .put(Stations.TYPE, station.getType())
                    .put(Stations.TITLE, station.getTitle())
                    .put(Stations.PERMALINK, station.getPermalink());

            final Optional<String> artworkUrlTemplate = station.getArtworkUrlTemplate();
            contentValuesBuilder.put(Stations.ARTWORK_URL_TEMPLATE,
                                     artworkUrlTemplate.isPresent() ?
                                     artworkUrlTemplate.get() :
                                     null);
            return contentValuesBuilder.get();
        }
    };

    void setLikedStationsAndAddNewMetaData(final List<Urn> remoteLikedStations,
                                           final List<ApiStationMetadata> newStationsMetaData) {
        propellerDatabase.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propellerDatabase) {
                final Where filterLikedStations = filter().whereEq(StationsCollections.COLLECTION_TYPE,
                                                                   StationsCollectionsTypes.LIKED);
                step(propellerDatabase.bulkUpsert(Stations.TABLE, transform(newStationsMetaData, TO_STATION_METADATA)));
                step(propellerDatabase.delete(StationsCollections.TABLE, filterLikedStations));
                step(propellerDatabase.bulkUpsert(StationsCollections.TABLE, toContentValues(remoteLikedStations)));
            }
        });
    }

    private static int mapLastPosition(CursorReader cursorReader) {
        return cursorReader.isNull(Stations.LAST_PLAYED_TRACK_POSITION) ?
               NEVER_PLAYED : cursorReader.getInt(Stations.LAST_PLAYED_TRACK_POSITION);
    }

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
                    StationProperty.ADDED_AT.bind(reader.getLong(StationsCollections.ADDED_AT)),
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

    Observable<ChangeResult> updateLocalStationLike(Urn stationUrn, boolean liked) {
        return propellerRx.upsert(StationsCollections.TABLE, contentValuesForStationLikeToggled(stationUrn, liked));
    }

    private ContentValues contentValuesForStationLikeToggled(Urn stationUrn, boolean liked) {
        return ContentValuesBuilder.values()
                                   .put(StationsCollections.STATION_URN, stationUrn.toString())
                                   .put(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.LIKED)
                                   .put(liked ? StationsCollections.ADDED_AT : StationsCollections.REMOVED_AT,
                                        dateProvider.getCurrentTime())
                                   .put(liked ? StationsCollections.REMOVED_AT : StationsCollections.ADDED_AT, null)
                                   .get();
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

    List<Urn> getStations() {
        return propellerDatabase
                .query(Query.from(Stations.TABLE).select(Stations.STATION_URN))
                .toList(STATIONS_TO_URN);
    }

    private Query buildStationsQuery(int collectionType) {
        return Query.from(StationsCollections.TABLE)
                    .whereEq(StationsCollections.COLLECTION_TYPE, collectionType)
                    .whereNull(StationsCollections.REMOVED_AT)
                    .order(StationsCollections.ADDED_AT, Query.Order.DESC)
                    .order(StationsCollections.POSITION, Query.Order.ASC);
    }

    Observable<StationRecord> station(Urn stationUrn) {
        return Observable.zip(
                propellerRx.query(Query.from(Stations.TABLE).whereEq(Stations.STATION_URN, stationUrn))
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

    Observable<StationWithTracks> stationWithTracks(final Urn station) {
        return propellerRx.query(Query.from(Stations.TABLE)
                                      .select(Stations.STATION_URN,
                                              Stations.TITLE,
                                              Stations.TYPE,
                                              Stations.LAST_PLAYED_TRACK_POSITION,
                                              Stations.ARTWORK_URL_TEMPLATE,
                                              Stations.PERMALINK,
                                              exists(likeQuery(station)).as(STATION_LIKE))
                                      .whereEq(Stations.STATION_URN, station))
                          .map(new StationWithTracksMapper())
                          .zipWith(stationTracks(station).toList(), ZIP_STATION_WITH_TRACKS);
    }

    private static Query likeQuery(Urn stationUrn) {
        return Query.from(StationsCollections.TABLE)
                    .whereNull(StationsCollections.REMOVED_AT)
                    .whereEq(StationsCollections.STATION_URN, stationUrn)
                    .whereEq(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.LIKED);
    }

    Observable<StationInfoTrack> stationTracks(Urn stationUrn) {
        final Query query = Query.from(Table.SoundView.name())
                                 .innerJoin(StationsPlayQueues.TABLE.name(),
                                            SoundView._ID,
                                            StationsPlayQueues.TRACK_ID.name())
                                 .select(SoundView._ID,
                                         SoundView.TITLE,
                                         SoundView.USERNAME,
                                         SoundView.USER_ID,
                                         SoundView.PLAYBACK_COUNT,
                                         SoundView.ARTWORK_URL)
                                 .whereEq(SoundView._TYPE, TableColumns.Sounds.TYPE_TRACK)
                                 .whereEq(StationsPlayQueues.STATION_URN, stationUrn.toString())
                                 .order(StationsPlayQueues.POSITION, Query.Order.ASC);
        return propellerRx.query(query).map(new StationInfoTrackMapper());
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
                        .put(StationsCollections.ADDED_AT, dateProvider.getCurrentTime())
                        .get()
        );
    }

    List<PropertySet> getRecentStationsToSync() {
        return propellerDatabase
                .query(Query.from(StationsCollections.TABLE)
                            .whereEq(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.RECENT)
                            .whereNotNull(StationsCollections.ADDED_AT))
                .toList(TO_RECENT_STATION);
    }

    List<Urn> getLocalLikedStations() {
        return getChangedLikedStation(StationsCollections.ADDED_AT);
    }

    List<Urn> getLocalUnlikedStations() {
        return getChangedLikedStation(StationsCollections.REMOVED_AT);
    }

    private List<Urn> getChangedLikedStation(Column column) {
        return propellerDatabase.query(Query.from(StationsCollections.TABLE)
                                            .whereEq(StationsCollections.COLLECTION_TYPE,
                                                     StationsCollectionsTypes.LIKED)
                                            .whereNotNull(column))
                                .toList(STATIONS_COLLECTIONS_TO_URN);
    }

    @NonNull
    private List<ContentValues> toContentValues(List<Urn> likedStations) {
        final List<ContentValues> contentValuesList = new ArrayList<>(likedStations.size());
        for (int i = 0; i < likedStations.size(); i++) {
            final Urn station = likedStations.get(i);

            final ContentValues contentValues = values()
                    .put(StationsCollections.STATION_URN, station.toString())
                    .put(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.LIKED)
                    .put(StationsCollections.POSITION, i)
                    .get();
            contentValuesList.add(contentValues);
        }
        return contentValuesList;
    }

    boolean isOnboardingStreamItemDisabled() {
        return sharedPreferences.getBoolean(ONBOARDING_STREAM_ITEM_DISABLED, false);
    }

    void disableOnboardingStreamItem() {
        sharedPreferences.edit().putBoolean(ONBOARDING_STREAM_ITEM_DISABLED, true).apply();
    }

    boolean isOnboardingForLikedStationsDisabled() {
        return sharedPreferences.getBoolean(ONBOARDING_LIKED_STATIONS_DISABLED, false);
    }

    void disableLikedStationsOnboarding() {
        sharedPreferences.edit().putBoolean(ONBOARDING_LIKED_STATIONS_DISABLED, true).apply();
    }

    boolean shouldRunRecentToLikedMigration() {
        return sharedPreferences.getBoolean(MIGRATE_RECENT_TO_LIKED_STATIONS, true);
    }

    void markRecentToLikedMigrationComplete() {
        sharedPreferences.edit().putBoolean(MIGRATE_RECENT_TO_LIKED_STATIONS, false).apply();
    }

    private final class StationInfoTrackMapper extends RxResultMapper<StationInfoTrack> {

        @Override
        public StationInfoTrack map(CursorReader reader) {
            return StationInfoTrack.from(
                    PropertySet.from(
                            URN.bind(Urn.forTrack(reader.getLong(SoundView._ID))),
                            TITLE.bind(reader.getString(SoundView.TITLE)),
                            CREATOR_NAME.bind(reader.getString(SoundView.USERNAME)),
                            CREATOR_URN.bind(Urn.forUser(reader.getLong(SoundView.USER_ID))),
                            PLAY_COUNT.bind(reader.getInt(SoundView.PLAYBACK_COUNT)),
                            IMAGE_URL_TEMPLATE.bind(fromNullable(reader.getString(SoundView.ARTWORK_URL)))
                    )
            );
        }
    }

    private final class StationWithTracksMapper extends RxResultMapper<StationWithTracks> {

        @Override
        public StationWithTracks map(CursorReader reader) {
            return new StationWithTracks(
                    new Urn(reader.getString(Stations.STATION_URN)),
                    reader.getString(Stations.TITLE),
                    reader.getString(Stations.TYPE),
                    Optional.fromNullable(reader.getString(Stations.ARTWORK_URL_TEMPLATE)),
                    reader.getString(Stations.PERMALINK),
                    Collections.<StationInfoTrack>emptyList(),
                    reader.getInt(Stations.LAST_PLAYED_TRACK_POSITION),
                    reader.getBoolean(STATION_LIKE)
            );
        }
    }
}
