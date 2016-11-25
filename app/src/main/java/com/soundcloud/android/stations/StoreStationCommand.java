package com.soundcloud.android.stations;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Tables.Stations;
import com.soundcloud.android.storage.Tables.StationsPlayQueues;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.schema.BulkInsertValues;
import com.soundcloud.propeller.schema.Column;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

class StoreStationCommand extends DefaultWriteStorageCommand<StationRecord, WriteResult> {

    private final DateProvider dateProvider;

    @Inject
    public StoreStationCommand(PropellerDatabase database, CurrentDateProvider dateProvider) {
        super(database);
        this.dateProvider = dateProvider;
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final StationRecord station) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.insert(Stations.TABLE, buildStationContentValues(station)));
                final Integer playQueueSize = queryPlayQueueSize(propeller);
                step(propeller.bulkInsert(StationsPlayQueues.TABLE, playQueueBulkValues(playQueueSize)));
            }

            private Integer queryPlayQueueSize(PropellerDatabase propeller) {
                final Query query = Query.count(StationsPlayQueues.TABLE)
                                         .whereEq(StationsPlayQueues.STATION_URN, station.getUrn().toString());
                return propeller.query(query).first(Integer.class);
            }

            private BulkInsertValues playQueueBulkValues(Integer playQueueSize) {
                BulkInsertValues.Builder builder = new BulkInsertValues.Builder(columns());
                final List<StationTrack> tracks = station.getTracks();
                for (int position = 0; position < tracks.size(); position++) {
                    builder.addRow(buildRow(station,
                                            tracks.get(position),
                                            playQueueSize + position));
                }
                return builder.build();
            }

            private List<Column> columns() {
                return Arrays.asList(
                        StationsPlayQueues.STATION_URN,
                        StationsPlayQueues.TRACK_ID,
                        StationsPlayQueues.QUERY_URN,
                        StationsPlayQueues.POSITION
                );
            }

        });
    }

    private List<Object> buildRow(StationRecord station, StationTrack stationTrack, int trackPosition) {
        return Arrays.<Object>asList(
                station.getUrn().toString(),
                stationTrack.getTrackUrn().getNumericId(),
                stationTrack.getQueryUrn().toString(),
                trackPosition
        );
    }

    private ContentValues buildStationContentValues(StationRecord station) {
        return ContentValuesBuilder
                .values()
                .put(Stations.STATION_URN, station.getUrn().toString())
                .put(Stations.TYPE, station.getType())
                .put(Stations.TITLE, station.getTitle())
                .put(Stations.PERMALINK, station.getPermalink())
                .put(Stations.PLAY_QUEUE_UPDATED_AT, dateProvider.getCurrentTime())
                .put(Stations.ARTWORK_URL_TEMPLATE, station.getImageUrlTemplate().orNull())
                .get();
    }
}
