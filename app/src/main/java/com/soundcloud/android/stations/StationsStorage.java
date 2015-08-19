package com.soundcloud.android.stations;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.RecentStations;
import com.soundcloud.android.storage.Tables.Stations;
import com.soundcloud.android.storage.Tables.StationsPlayQueues;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

class StationsStorage {
    private static final Func1<CursorReader, Station> toStationWithoutTracks = new Func1<CursorReader, Station>() {
        @Override
        public Station call(CursorReader cursorReader) {
            return new Station(
                    new Urn(cursorReader.getString(Stations.STATION_URN)),
                    cursorReader.getString(Stations.TITLE),
                    cursorReader.getString(Stations.TYPE),
                    Collections.<Urn>emptyList(),
                    cursorReader.getInt(Stations.LAST_PLAYED_TRACK_POSITION)
            );
        }
    };

    private static final Func1<CursorReader, Urn> toTrackUrn = new Func1<CursorReader, Urn>() {
        @Override
        public Urn call(CursorReader cursorReader) {
            return new Urn(cursorReader.getString(StationsPlayQueues.TRACK_URN));
        }
    };

    private final Func1<CursorReader, Observable<Station>> toStation = new Func1<CursorReader, Observable<Station>>() {
        @Override
        public Observable<Station> call(CursorReader cursorReader) {
            return station(new Urn(cursorReader.getString(Stations.STATION_URN)));
        }
    };
    
    private final PropellerRx propellerRx;
    private final DateProvider dateProvider;

    @Inject
    public StationsStorage(PropellerRx propellerRx, DateProvider dateProvider) {
        this.propellerRx = propellerRx;
        this.dateProvider = dateProvider;
    }

    Observable<Station> recentStations() {
        return propellerRx
                .query(buildRecentStationsQuery())
                .flatMap(toStation);
    }

    private Query buildRecentStationsQuery() {
        return Query
                .from(RecentStations.TABLE)
                .order(RecentStations.STARTED_AT, Query.Order.DESC);
    }

    Observable<Station> station(Urn stationUrn) {
        return Observable.zip(
                propellerRx.query(Query.from(Stations.TABLE).whereEq(Stations.STATION_URN, stationUrn)).map(toStationWithoutTracks),
                propellerRx.query(buildTracksListQuery(stationUrn)).map(toTrackUrn).toList(),
                new Func2<Station, List<Urn>, Station>() {
                    @Override
                    public Station call(Station station, List<Urn> tracks) {
                        return new Station(
                                station.getUrn(),
                                station.getTitle(),
                                station.getType(),
                                tracks,
                                calcStartPosition(station.getStartPosition(), tracks.size())
                        );
                    }
                }
        );
    }

    private Query buildTracksListQuery(Urn stationUrn) {
        return Query
                .from(StationsPlayQueues.TABLE)
                .select(StationsPlayQueues.TRACK_URN)
                .whereEq(StationsPlayQueues.STATION_URN, stationUrn)
                .order(StationsPlayQueues.POSITION, Query.Order.ASC);
    }

    // This is a temporary logic.
    // A story is going to be played to actually fetch more tracks.
    private Integer calcStartPosition(int lastPlayedTrackPosition, int numTracks) {
        if (lastPlayedTrackPosition + 1 < numTracks) {
            return lastPlayedTrackPosition + 1;
        } else {
            return 0;
        }
    }

    Observable<ChangeResult> saveLastPlayedTrackPosition(Urn stationUrn, int position) {
        return propellerRx.update(
                Stations.TABLE,
                ContentValuesBuilder.values().put(Stations.LAST_PLAYED_TRACK_POSITION, position).get(),
                filter().whereEq(Stations.STATION_URN, stationUrn.toString())
        );
    }

    Observable<ChangeResult> saveRecentlyPlayedStation(Urn stationUrn) {
        return propellerRx.upsert(
                RecentStations.TABLE,
                ContentValuesBuilder
                        .values()
                        .put(RecentStations.STATION_URN, stationUrn.toString())
                        .put(RecentStations.STARTED_AT, dateProvider.getCurrentTime())
                        .get()
        );
    }
}
