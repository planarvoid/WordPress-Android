package com.soundcloud.android.waveform;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.DateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;

import java.util.Collections;

public class WaveformStorageTest extends StorageIntegrationTest {

    private final Urn track = Urn.forTrack(123L);

    private WaveformStorage storage;
    private TestObserver<WaveformData> testObserver;

    @Mock DateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        testObserver = new TestObserver<>();
        storage = new WaveformStorage(propellerRx(), dateProvider, new WaveformSerializer());
    }

    @Test
    public void storeWritesToWaveformTable() {
        Urn track = Urn.forTrack(123L);
        WaveformData waveformData = new WaveformData(12, new int[]{23, 123, 123});

        storage.store(track, waveformData).subscribe();

        databaseAssertions().assertWaveformForTrack(track, waveformData);
    }

    @Test
    public void loadRetrievesWaveformData() {
        WaveformData waveformData = new WaveformData(12, new int[]{23, 123, 123});

        storage.store(track, waveformData).subscribe();

        storage.load(track).subscribe(testObserver);
        testObserver.assertReceivedOnNext(Collections.singletonList(waveformData));
    }

    @Test
    public void loadReturnsEmptyWaveformDataWhenNotPresent() {
        WaveformData waveformData = new WaveformData(12, new int[]{23, 123, 123});

        storage.load(track)
                .switchIfEmpty(Observable.just(waveformData))
                .subscribe(testObserver);

        testObserver.assertReceivedOnNext(Collections.singletonList(waveformData));
    }
}