package com.soundcloud.android.stations;

import static com.soundcloud.propeller.query.Filter.filter;

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

public class WriteStationsRecommendationsCommand
        extends WriteStorageCommand<List<StationRecord>, TxnResult, Boolean> {

    private static final int STATIONS_COLLECTIONS_TYPE = StationsCollectionsTypes.SUGGESTIONS;

    private final static Function<StationRecord, ContentValues> TO_CONTENT_VALUES =
            new Function<StationRecord, ContentValues>() {
                @Override
                public ContentValues apply(StationRecord station) {
                    return buildStationContentValues(station);
                }
            };

    @Inject
    WriteStationsRecommendationsCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, final List<StationRecord> stations) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.delete(
                        Tables.StationsCollections.TABLE,
                        filter().whereEq(Tables.StationsCollections.COLLECTION_TYPE, STATIONS_COLLECTIONS_TYPE)
                ));

                step(saveStations(propeller, stations));
                step(addStationsToCollection(propeller, STATIONS_COLLECTIONS_TYPE, stations));
            }
        });
    }

    private TxnResult saveStations(PropellerDatabase propeller, List<StationRecord> stations) {
        return propeller.bulkUpsert(Tables.Stations.TABLE, Lists.transform(stations, TO_CONTENT_VALUES));
    }

    private TxnResult addStationsToCollection(PropellerDatabase propeller, int type, List<StationRecord> stations) {
        return propeller.bulkInsert(
                Tables.StationsCollections.TABLE,
                toStationsCollectionsContentValues(stations, type),
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    private List<ContentValues> toStationsCollectionsContentValues(List<StationRecord> stations, int type) {
        List<ContentValues> stationsToSave = new ArrayList<>();
        for (int i = 0; i < stations.size(); i++) {
            stationsToSave.add(buildStationsCollectionsItemContentValues(stations.get(i), type, i));
        }
        return stationsToSave;
    }

    private ContentValues buildStationsCollectionsItemContentValues(StationRecord station, int type, int position) {
        return ContentValuesBuilder
                .values()
                .put(Tables.StationsCollections.STATION_URN, station.getUrn().toString())
                .put(Tables.StationsCollections.COLLECTION_TYPE, type)
                .put(Tables.StationsCollections.POSITION, position)
                .get();
    }

    private static ContentValues buildStationContentValues(StationRecord station) {
        return ContentValuesBuilder
                .values()
                .put(Tables.Stations.STATION_URN, station.getUrn().toString())
                .put(Tables.Stations.TYPE, station.getType())
                .put(Tables.Stations.TITLE, station.getTitle())
                .put(Tables.Stations.PERMALINK, station.getPermalink())
                .put(Tables.Stations.ARTWORK_URL_TEMPLATE, station.getImageUrlTemplate().orNull())
                .get();
    }

    @Override
    protected Boolean transform(TxnResult result) {
        return result.success();
    }
}
