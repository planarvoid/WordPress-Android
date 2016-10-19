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

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
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
                step(propeller.bulkInsert_experimental(StationsPlayQueues.TABLE,
                                                       columnTypes(),
                                                       contentValues(playQueueSize)));
            }

            private Integer queryPlayQueueSize(PropellerDatabase propeller) {
                final Query query = Query.count(StationsPlayQueues.TABLE)
                                         .whereEq(StationsPlayQueues.STATION_URN, station.getUrn().toString());
                return propeller.query(query).first(Integer.class);
            }

            private HashMap<String, Class> columnTypes() {
                final HashMap<String, Class> columns = new HashMap<>();
                columns.put(StationsPlayQueues.STATION_URN.name(), String.class);
                columns.put(StationsPlayQueues.TRACK_ID.name(), Long.class);
                columns.put(StationsPlayQueues.QUERY_URN.name(), String.class);
                columns.put(StationsPlayQueues.POSITION.name(), Long.class);
                return columns;
            }

            private List<ContentValues> contentValues(Integer playQueueSize) {
                final List<StationTrack> tracks = station.getTracks();

                final List<ContentValues> contentValues = new ArrayList<>(tracks.size());
                for (int position = 0; position < tracks.size(); position++) {
                    contentValues.add(buildContentValues(station,
                                                         tracks.get(position),
                                                         playQueueSize + position));
                }
                return contentValues;
            }

        });
    }

    private ContentValues buildContentValues(StationRecord station, StationTrack stationTrack, int trackPosition) {
        return ContentValuesBuilder
                .values()
                .put(StationsPlayQueues.STATION_URN, station.getUrn().toString())
                .put(StationsPlayQueues.TRACK_ID, stationTrack.getTrackUrn().getNumericId())
                .put(StationsPlayQueues.QUERY_URN, stationTrack.getQueryUrn().toString())
                .put(StationsPlayQueues.POSITION, trackPosition)
                .get();
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
