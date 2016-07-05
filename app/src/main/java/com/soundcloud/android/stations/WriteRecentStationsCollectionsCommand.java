package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationsCollectionsTypes.RECENT;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class WriteRecentStationsCollectionsCommand
        extends WriteStorageCommand<WriteRecentStationsCollectionsCommand.SyncCollectionsMetadata, TxnResult, Boolean> {

    private final static Function<ApiStationMetadata, ContentValues> TO_CONTENT_VALUES =
            new Function<ApiStationMetadata, ContentValues>() {
                @Override
                public ContentValues apply(ApiStationMetadata station) {
                    return buildStationContentValues(station);
                }
            };

    @Inject
    WriteRecentStationsCollectionsCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, final SyncCollectionsMetadata metadata) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(deleteLocalContentSynced(propeller));
                step(deleteStaleContent(propeller));

                List<ApiStationMetadata> stations = metadata.stationsCollections.getRecents();
                step(saveStations(propeller, stations));
                step(addStationsToCollection(propeller, RECENT, stations));
            }

            private ChangeResult deleteStaleContent(PropellerDatabase propeller) {
                return propeller.delete(
                        Tables.StationsCollections.TABLE,
                        filter()
                                .whereEq(Tables.StationsCollections.COLLECTION_TYPE, RECENT)
                                .whereNull(Tables.StationsCollections.UPDATED_LOCALLY_AT)
                );
            }

            private ChangeResult deleteLocalContentSynced(PropellerDatabase propeller) {
                return propeller.delete(
                        Tables.StationsCollections.TABLE,
                        filter()
                                .whereEq(Tables.StationsCollections.COLLECTION_TYPE, RECENT)
                                .whereLt(Tables.StationsCollections.UPDATED_LOCALLY_AT, metadata.clearBeforeTime)
                );
            }

        });
    }


    private TxnResult saveStations(PropellerDatabase propeller, List<ApiStationMetadata> stations) {
        return propeller.bulkUpsert(Tables.Stations.TABLE, Lists.transform(stations, TO_CONTENT_VALUES));
    }

    private TxnResult addStationsToCollection(PropellerDatabase propeller,
                                              int type,
                                              List<ApiStationMetadata> stations) {
        return propeller.bulkInsert(
                Tables.StationsCollections.TABLE,
                toStationsCollectionsContentValues(stations, type),
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    private List<ContentValues> toStationsCollectionsContentValues(List<ApiStationMetadata> stations, int type) {
        List<ContentValues> stationsToSave = new ArrayList<>();
        for (int i = 0; i < stations.size(); i++) {
            stationsToSave.add(buildStationsCollectionsItemContentValues(stations.get(i), type, i));
        }
        return stationsToSave;
    }

    private ContentValues buildStationsCollectionsItemContentValues(ApiStationMetadata station,
                                                                    int type,
                                                                    int position) {
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
        final ContentValuesBuilder contentValuesBuilder = ContentValuesBuilder
                .values()
                .put(Tables.Stations.STATION_URN, station.getUrn().toString())
                .put(Tables.Stations.TYPE, station.getType())
                .put(Tables.Stations.TITLE, station.getTitle())
                .put(Tables.Stations.PERMALINK, station.getPermalink());

        final Optional<String> artworkUrlTemplate = station.getArtworkUrlTemplate();
        contentValuesBuilder.put(Tables.Stations.ARTWORK_URL_TEMPLATE, artworkUrlTemplate.isPresent() ?
                                                                       artworkUrlTemplate.get() :
                                                                       null);
        return contentValuesBuilder.get();
    }

    static final class SyncCollectionsMetadata {
        private final long clearBeforeTime;
        private final ApiStationsCollections stationsCollections;

        SyncCollectionsMetadata(long clearBeforeTime, ApiStationsCollections stationsCollections) {
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
            return MoreObjects.equal(clearBeforeTime, that.clearBeforeTime) &&
                    MoreObjects.equal(stationsCollections, that.stationsCollections);
        }

        @Override
        public int hashCode() {
            return MoreObjects.hashCode(clearBeforeTime, stationsCollections);
        }
    }
}
