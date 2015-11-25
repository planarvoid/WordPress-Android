package com.soundcloud.android.stations;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Tables.Stations;
import com.soundcloud.android.storage.Tables.StationsPlayQueues;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Query;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.List;

class StoreStationCommand extends DefaultWriteStorageCommand<StationRecord, WriteResult> {

    @Inject
    public StoreStationCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final StationRecord station) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.upsert(Stations.TABLE, buildStationContentValues(station)));
                addPlayQueue(propeller);
            }

            private void addPlayQueue(PropellerDatabase propeller) {
                final List<StationTrack> tracks = station.getTracks();
                final Integer playQueueSize = propeller.query(Query.count(StationsPlayQueues.TABLE)
                        .whereEq(StationsPlayQueues.STATION_URN, station.getUrn().toString()))
                        .first(Integer.class);

                for (int position = 0; position < tracks.size(); position++) {
                    step(propeller.upsert(StationsPlayQueues.TABLE, buildContentValues(station, tracks.get(position), playQueueSize + position)));
                }
            }
        });
    }

    private ContentValues buildContentValues(StationRecord station, StationTrack stationTrack, int trackPosition) {
        return ContentValuesBuilder
                .values()
                .put(StationsPlayQueues.STATION_URN, station.getUrn().toString())
                .put(StationsPlayQueues.TRACK_URN, stationTrack.getTrackUrn().toString())
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
                .get();
    }
}
