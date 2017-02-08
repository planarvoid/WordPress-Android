package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.Stations.NEVER_PLAYED;
import static com.soundcloud.java.collections.Lists.transform;
import static com.soundcloud.java.optional.Optional.fromNullable;
import static com.soundcloud.propeller.ContentValuesBuilder.values;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.apply;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.storage.Tables.Stations;
import com.soundcloud.android.storage.Tables.StationsCollections;
import com.soundcloud.android.storage.Tables.StationsPlayQueues;
import com.soundcloud.android.storage.Tables.TrackView;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import com.soundcloud.propeller.schema.BulkInsertValues;
import com.soundcloud.propeller.schema.Column;
import rx.Observable;

import android.content.ContentValues;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

class StationsStorage {

    private static final String STATION_LIKE = "STATION_LIKE";
    private static final String ONBOARDING_LIKED_STATIONS_DISABLED = "ONBOARDING_LIKED_STATIONS_DISABLED";
    private static final String ONBOARDING_STREAM_ITEM_DISABLED = "ONBOARDING_STREAM_ITEM_DISABLED";
    private static final String MIGRATE_RECENT_TO_LIKED_STATIONS = "MIGRATE_RECENT_TO_LIKED_STATIONS";
    private static final long EXPIRE_DELAY = TimeUnit.HOURS.toMillis(24);

    void setLikedStationsAndAddNewMetaData(final List<Urn> remoteLikedStations,
                                           final List<ApiStationMetadata> newStationsMetaData) {
        propellerDatabase.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propellerDatabase) {
                final Where filterLikedStations = filter().whereEq(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.LIKED);

                step(storeStationsMetadata(newStationsMetaData));
                step(propellerDatabase.delete(StationsCollections.TABLE, filterLikedStations));
                step(propellerDatabase.bulkInsert(StationsCollections.TABLE, toBulkValues(remoteLikedStations)));
            }
        });
    }

    WriteResult storeStationsMetadata(List<ApiStationMetadata> newStationsMetaData) {
        return propellerDatabase.bulkUpsert(Stations.TABLE, transform(newStationsMetaData, station -> {
            final ContentValuesBuilder contentValuesBuilder = values()
                    .put(Stations.STATION_URN, station.getUrn().toString())
                    .put(Stations.TYPE, station.getType())
                    .put(Stations.TITLE, station.getTitle())
                    .put(Stations.PERMALINK, station.getPermalink());

            final Optional<String> artworkUrlTemplate = station.getArtworkUrlTemplate();
            contentValuesBuilder.put(Stations.ARTWORK_URL_TEMPLATE, artworkUrlTemplate.isPresent() ? artworkUrlTemplate.get() : null);
            return contentValuesBuilder.get();
        }));
    }

    private static int mapLastPosition(CursorReader cursorReader) {
        return cursorReader.isNull(Stations.LAST_PLAYED_TRACK_POSITION) ?
               NEVER_PLAYED : cursorReader.getInt(Stations.LAST_PLAYED_TRACK_POSITION);
    }

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
                .map(new StationTrackMapper());
    }

    Observable<TxnResult> clearExpiredPlayQueue(final Urn stationUrn) {
        return propellerRx.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                final Query isPlayQueueExpired = apply(exists(Query.from(Stations.TABLE)
                                                                   .whereEq(Stations.STATION_URN, stationUrn.toString())
                                                                   .whereLe(Stations.PLAY_QUEUE_UPDATED_AT, dateProvider.getCurrentTime() - EXPIRE_DELAY)));

                if (propeller.query(isPlayQueueExpired).first(Boolean.class)) {
                    step(propeller.delete(StationsPlayQueues.TABLE, filter().whereEq(StationsPlayQueues.STATION_URN, stationUrn.toString())));
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
                                   .put(liked ? StationsCollections.ADDED_AT : StationsCollections.REMOVED_AT, dateProvider.getCurrentTime())
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
                .flatMap(cursorReader -> station(new Urn(cursorReader.getString(Stations.STATION_URN))));
    }

    List<Urn> getStations() {
        return propellerDatabase
                .query(Query.from(Stations.TABLE).select(Stations.STATION_URN))
                .toList(reader -> new Urn(reader.getString(Stations.STATION_URN)));
    }

    private Query buildStationsQuery(int collectionType) {
        return Query.from(StationsCollections.TABLE)
                    .whereEq(StationsCollections.COLLECTION_TYPE, collectionType)
                    .whereNull(StationsCollections.REMOVED_AT)
                    .order(StationsCollections.ADDED_AT, Query.Order.DESC)
                    .order(StationsCollections.POSITION, Query.Order.ASC);
    }

    Observable<StationRecord> station(final Urn station) {
        return Observable.fromCallable(() -> {
            final Station result = propellerDatabase
                    .query(Query.from(Stations.TABLE).whereEq(Stations.STATION_URN, station))
                    .firstOrDefault(new StationMapper(), null);

            if (result != null) {
                final List<StationTrack> stationTracks = propellerDatabase.query(buildTracksListQuery(station)).toList(new StationTrackMapper());

                return Station.stationWithTracks(result, stationTracks);
            }
            return null;
        });
    }

    Observable<StationWithTrackUrns> stationWithTrackUrns(final Urn station) {
        return Observable.fromCallable(() -> {
            final StationWithTrackUrns stationWithTracks = propellerDatabase.query(stationInfoQuery(station))
                                                                            .firstOrDefault(new StationWithTracksMapper(), null);

            if (stationWithTracks != null) {
                final List<Urn> trackUrns = propellerDatabase.query(stationInfoTracksQuery(station)).toList(new StationTrackUrnMapper());
                return stationWithTracks.copyWithTrackUrns(trackUrns);
            }
            return stationWithTracks;
        });
    }

    private static Query stationInfoTracksQuery(Urn station) {
        return Query.from(TrackView.TABLE)
                    .innerJoin(StationsPlayQueues.TABLE.name(), TrackView.ID.qualifiedName(), StationsPlayQueues.TRACK_ID.name())
                    .select(TrackView.ID,
                            TrackView.TITLE,
                            TrackView.CREATOR_NAME,
                            TrackView.CREATOR_ID,
                            TrackView.SNIPPET_DURATION,
                            TrackView.FULL_DURATION,
                            TrackView.SNIPPED,
                            TrackView.IS_USER_LIKE,
                            TrackView.IS_USER_REPOST,
                            TrackView.LIKES_COUNT,
                            TrackView.PERMALINK_URL,
                            TrackView.PLAY_COUNT,
                            TrackView.ARTWORK_URL)
                    .whereEq(StationsPlayQueues.STATION_URN, station.toString())
                    .order(StationsPlayQueues.POSITION, Query.Order.ASC);
    }

    private static Query stationInfoQuery(Urn station) {
        return Query.from(Stations.TABLE)
                    .select(Stations.STATION_URN,
                            Stations.TITLE,
                            Stations.TYPE,
                            Stations.LAST_PLAYED_TRACK_POSITION,
                            Stations.ARTWORK_URL_TEMPLATE,
                            Stations.PERMALINK,
                            exists(likeQuery(station)).as(STATION_LIKE))
                    .whereEq(Stations.STATION_URN, station);
    }

    private static Query likeQuery(Urn stationUrn) {
        return Query.from(StationsCollections.TABLE)
                    .whereNull(StationsCollections.REMOVED_AT)
                    .whereEq(StationsCollections.STATION_URN, stationUrn)
                    .whereEq(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.LIKED);
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
                                .toList(reader -> new Urn(reader.getString(StationsCollections.STATION_URN)));
    }

    private BulkInsertValues toBulkValues(List<Urn> likedStations) {
        BulkInsertValues.Builder builder = new BulkInsertValues.Builder(
                Arrays.asList(
                        StationsCollections.STATION_URN,
                        StationsCollections.COLLECTION_TYPE,
                        StationsCollections.POSITION
                )
        );

        for (int i = 0; i < likedStations.size(); i++) {
            final Urn station = likedStations.get(i);
            builder.addRow(Arrays.asList(
                    station.toString(),
                    StationsCollectionsTypes.LIKED,
                    i
            ));
        }
        return builder.build();
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

    private final class StationTrackUrnMapper extends RxResultMapper<Urn> {
        @Override
        public Urn map(CursorReader reader) {
            return Urn.forTrack(reader.getLong(TrackView.ID));
        }
    }

    private final class StationWithTracksMapper extends RxResultMapper<StationWithTrackUrns> {

        @Override
        public StationWithTrackUrns map(CursorReader reader) {
            return StationWithTrackUrns.create(new Urn(reader.getString(Stations.STATION_URN)),
                                               reader.getString(Stations.TYPE),
                                               reader.getString(Stations.TITLE),
                                               fromNullable(reader.getString(Stations.PERMALINK)),
                                               Optional.fromNullable(reader.getString(Stations.ARTWORK_URL_TEMPLATE)),
                                               reader.getInt(Stations.LAST_PLAYED_TRACK_POSITION),
                                               reader.getBoolean(STATION_LIKE)
            );
        }
    }

    private final class StationMapper extends RxResultMapper<Station> {
        @Override
        public Station map(CursorReader cursorReader) {
            return new Station(new Urn(cursorReader.getString(Stations.STATION_URN)),
                               cursorReader.getString(Stations.TITLE),
                               cursorReader.getString(Stations.TYPE),
                               Collections.emptyList(),
                               cursorReader.getString(Stations.PERMALINK),
                               mapLastPosition(cursorReader),
                               fromNullable(cursorReader.getString(Stations.ARTWORK_URL_TEMPLATE)));
        }
    }

    private final class StationTrackMapper extends RxResultMapper<StationTrack> {
        @Override
        public StationTrack map(CursorReader cursorReader) {
            return StationTrack.create(Urn.forTrack(cursorReader.getLong(StationsPlayQueues.TRACK_ID)),
                                       new Urn(cursorReader.getString(StationsPlayQueues.QUERY_URN))
            );
        }
    }

}
