package com.soundcloud.android.stations;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.Syncable;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LikedStationsSyncProviderTest {

    private LikedStationsSyncProvider syncProvider;

    @Mock
    private LikedStationsSyncer likedStationsSyncer;
    @Mock
    private StationsStorage stationsStorage;

    @Before
    public void setUp() {
        syncProvider = new LikedStationsSyncProvider(providerOf(likedStationsSyncer), stationsStorage);
    }

    @Test
    public void shouldSyncCorrectEntityType() {
        assertThat(syncProvider.id()).isEqualTo(Syncable.LIKED_STATIONS.name());
    }

    @Test
    public void shouldNotBeOutOfSyncWithNoLocalChanges() {
        when(stationsStorage.getLocalLikedStations()).thenReturn(Lists.emptyList());
        when(stationsStorage.getLocalUnlikedStations()).thenReturn(Lists.emptyList());

        assertThat(syncProvider.isOutOfSync()).isFalse();
    }

    @Test
    public void shouldBeOutOfSyncWithLocalLikedChanges() {
        when(stationsStorage.getLocalLikedStations()).thenReturn(Lists.newArrayList(Urn.forTrackStation(1L)));
        when(stationsStorage.getLocalUnlikedStations()).thenReturn(Lists.emptyList());

        assertThat(syncProvider.isOutOfSync()).isTrue();
    }

    @Test
    public void shouldBeOutOfSyncWithLocalUnlikedChanges() {
        when(stationsStorage.getLocalLikedStations()).thenReturn(Lists.emptyList());
        when(stationsStorage.getLocalUnlikedStations()).thenReturn(Lists.newArrayList(Urn.forTrackStation(1L)));

        assertThat(syncProvider.isOutOfSync()).isTrue();
    }

    @Test
    public void shouldBeOutOfSyncWithLocalLikedAndUnlikedChanges() {
        when(stationsStorage.getLocalLikedStations()).thenReturn(Lists.newArrayList(Urn.forTrackStation(2L)));
        when(stationsStorage.getLocalUnlikedStations()).thenReturn(Lists.newArrayList(Urn.forTrackStation(1L)));

        assertThat(syncProvider.isOutOfSync()).isTrue();
    }
}
