package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.waveform.WaveformData;
import com.soundcloud.android.waveform.WaveformSerializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class WaveformStorageTest extends StorageIntegrationTest {

    private final Urn track = Urn.forTrack(123L);
    private final WaveformData waveformData = new WaveformData(12, new int[]{23, 123, 123});

    @Mock DateProvider dateProvider;

    private WaveformStorage storage;

    @Before
    public void setUp() {
        storage = new WaveformStorage(propeller(), dateProvider, new WaveformSerializer());
    }

    @Test
    public void hasWaveformReturnsTrueWhenWaveformExists() {
        storage.store(track, waveformData);
        assertThat(storage.hasWaveform(track)).isTrue();
    }

    @Test
    public void hasWaveformReturnsFalseWhenWaveformDoesNotExist() {
        assertThat(storage.hasWaveform(track)).isFalse();
    }

    @Test
    public void storeWritesToWaveformTable() {
        Urn track = Urn.forTrack(123L);
        WaveformData waveformData = new WaveformData(12, new int[]{23, 123, 123});

        storage.store(track, waveformData);

        databaseAssertions().assertWaveformForTrack(track, waveformData);
    }
}