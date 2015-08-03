package com.soundcloud.android.stations;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.api.model.ApiStation;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.java.functions.Function;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Collections;

public class StationsStorageTest extends StorageIntegrationTest {
    private final Function<TrackRecord, Urn> toUrn = new Function<TrackRecord, Urn>() {
        @Override
        public Urn apply(TrackRecord track) {
            return track.getUrn();
        }
    };
    private StationsStorage storage;
    private TestSubscriber<Station> subscriber = new TestSubscriber<>();

    @Before
    public void setup() {
        storage = new StationsStorage(propellerRx());
    }

    @Test
    public void shouldReturnEmptyIfStationIsAbsent() {
        storage.station(Urn.forTrackStation(999L)).subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertCompleted();
    }

    @Test
    public void shouldReturnTheStation() {
        ApiStation apiStation = testFixtures().insertStation(0);

        storage.station(apiStation.getInfo().getUrn()).subscribe(subscriber);

        final Station station = new Station(apiStation.getInfo().getUrn(), apiStation.getInfo().getTitle(), transform(apiStation.getTracks().getCollection(), toUrn), 0);
        subscriber.assertReceivedOnNext(Collections.singletonList(station));
    }

}