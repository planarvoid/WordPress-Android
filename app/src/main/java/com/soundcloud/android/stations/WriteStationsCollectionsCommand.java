package com.soundcloud.android.stations;

import static com.soundcloud.propeller.query.Filter.filter;

import autovalue.shaded.com.google.common.common.base.Objects;
import com.soundcloud.android.Consts;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class WriteStationsCollectionsCommand extends WriteStorageCommand<WriteStationsCollectionsCommand.SyncCollectionsMetadata, TxnResult, Boolean> {
    private final static Function<ApiStationMetadata, ContentValues> TO_CONTENT_VALUES = new Function<ApiStationMetadata, ContentValues>() {
        @Override
        public ContentValues apply(ApiStationMetadata station) {
            return buildStationContentValues(station);
        }
    };

    @Inject
    WriteStationsCollectionsCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, final SyncCollectionsMetadata metadata) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.delete(
                        Tables.StationsCollections.TABLE,
                        filter().whereLt(Tables.StationsCollections.UPDATED_LOCALLY_AT, metadata.clearBeforeTime)
                                .orWhereNull(Tables.StationsCollections.UPDATED_LOCALLY_AT)
                ));

                final ApiStationsCollections stationsCollections = metadata.stationsCollections;
                storeCollection(propeller, StationsCollectionsTypes.RECENT, stationsCollections.getRecents());
                storeCollection(propeller, StationsCollectionsTypes.SAVED, stationsCollections.getSaved());
                storeCollection(propeller, StationsCollectionsTypes.CURATOR_RECOMMENDATIONS, stationsCollections.getCuratorRecommendations());
                storeCollection(propeller, StationsCollectionsTypes.GENRE_RECOMMENDATIONS, stationsCollections.getGenreRecommendations());
                storeCollection(propeller, StationsCollectionsTypes.TRACK_RECOMMENDATIONS, stationsCollections.getTrackRecommendations());
            }

            private void storeCollection(PropellerDatabase propeller, int type, List<ApiStationMetadata> stations) {
                step(saveStations(propeller, stations));
                step(addStationsToCollection(propeller, type, stations));
            }
        });
    }


    private TxnResult saveStations(PropellerDatabase propeller, List<ApiStationMetadata> stations) {
        return propeller.bulkUpsert(Tables.Stations.TABLE, Lists.transform(stations, TO_CONTENT_VALUES));
    }

    private TxnResult addStationsToCollection(PropellerDatabase propeller, int type, List<ApiStationMetadata> stations) {
        return propeller.bulkInsert(
                Tables.StationsCollections.TABLE,
                toStationsCollectionsContentValues(stations, type),
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    private ArrayList<ContentValues> toStationsCollectionsContentValues(List<ApiStationMetadata> stations, int type) {
        ArrayList<ContentValues> stationsToSave = new ArrayList<>();
        for (int i = 0; i < stations.size(); i++) {
            stationsToSave.add(buildStationsCollectionsItemContentValues(stations.get(i), type, i));
        }
        return stationsToSave;
    }

    private ContentValues buildStationsCollectionsItemContentValues(ApiStationMetadata station, int type, int position) {
        return ContentValuesBuilder
                .values()
                .put(Tables.StationsCollections.STATION_URN, station.getUrn().toString())
                .put(Tables.StationsCollections.COLLECTION_TYPE, type)
                .put(Tables.StationsCollections.POSITION, position)
                .get();
    }

    @Override
    protected Boolean transform(TxnResult result) {
        return result.success();
    }

    private static ContentValues buildStationContentValues(ApiStationMetadata station) {
        return ContentValuesBuilder
                .values()
                .put(Tables.Stations.STATION_URN, station.getUrn().toString())
                .put(Tables.Stations.TYPE, station.getType())
                .put(Tables.Stations.TITLE, station.getTitle())
                .put(Tables.Stations.PERMALINK, station.getPermalink())
                .put(Tables.Stations.LAST_PLAYED_TRACK_POSITION, Consts.NOT_SET)
                .get();
    }

    static final class SyncCollectionsMetadata {
        private final long clearBeforeTime;
        private final ApiStationsCollections stationsCollections;

        public SyncCollectionsMetadata(long clearBeforeTime, ApiStationsCollections stationsCollections) {
            this.clearBeforeTime = clearBeforeTime;
            this.stationsCollections = stationsCollections;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            SyncCollectionsMetadata that = (SyncCollectionsMetadata) o;
            return Objects.equal(clearBeforeTime, that.clearBeforeTime) &&
                    Objects.equal(stationsCollections, that.stationsCollections);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(clearBeforeTime, stationsCollections);
        }
    }
}
