package com.soundcloud.android.stations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.Stations;
import com.soundcloud.android.storage.Tables.StationsPlayQueues;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import java.util.List;

class StationsStorage {

    private static final Func1<CursorReader, PropertySet> toStationsInfo = new Func1<CursorReader, PropertySet>() {
        @Override
        public PropertySet call(CursorReader cursorReader) {
            return PropertySet.from(
                    StationProperty.URN.bind(new Urn(cursorReader.getString(Stations.URN))),
                    StationProperty.TYPE.bind(cursorReader.getString(Stations.TYPE)),
                    StationProperty.TITLE.bind(cursorReader.getString(Stations.TITLE)),
                    StationProperty.SEED_TRACK_ID.bind(cursorReader.getLong(Stations.SEED_TRACK_ID)),
                    StationProperty.LAST_PLAYED_TRACK_POSITION.bind(cursorReader.getInt(Stations.LAST_PLAYED_TRACK_POSITION))
            );
        }
    };

    private static final Func1<CursorReader, Urn> toTrackUrn = new Func1<CursorReader, Urn>() {
        @Override
        public Urn call(CursorReader cursorReader) {
            return new Urn(cursorReader.getString(StationsPlayQueues.TRACK_URN));
        }
    };

    private final PropellerRx propellerRx;

    @Inject
    public StationsStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    Observable<Station> station(Urn stationUrn) {
        return Observable.zip(
                propellerRx.query(Query.from(Stations.TABLE).whereEq(Stations.URN, stationUrn)).map(toStationsInfo),
                propellerRx.query(getPlayQueueQuery(stationUrn)).map(toTrackUrn).toList(),
                new Func2<PropertySet, List<Urn>, Station>() {
                    @Override
                    public Station call(PropertySet info, List<Urn> tracks) {
                        return new Station(info, tracks);
                    }
                }
        );
    }

    private Query getPlayQueueQuery(Urn stationUrn) {
        return Query
                .from(StationsPlayQueues.TABLE)
                .select(StationsPlayQueues.TRACK_URN)
                .whereEq(StationsPlayQueues.STATION_URN, stationUrn)
                .order(StationsPlayQueues.POSITION, Query.Order.ASC);
    }
}
