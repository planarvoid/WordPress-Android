package com.soundcloud.android.stations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Provider;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class RecentStationsSyncProviderTest {

    private RecentStationsSyncProvider syncProvider;

    @Mock private Provider<RecentStationsSyncer> recentStationsSyncerProvider;
    @Mock private StationsStorage stationsStorage;

    @Before
    public void setUp() {
        this.syncProvider = new RecentStationsSyncProvider(recentStationsSyncerProvider, stationsStorage);
    }

    @Test
    public void shouldSyncCorrectEntityType() {
        assertThat(syncProvider.id()).isEqualTo(Syncable.RECENT_STATIONS.name());
    }

    @Test
    public void shouldBeOutOfSyncIfThereAreLocalChanges() {
        when(stationsStorage.getRecentStationsToSync()).thenReturn(Collections.singletonList(PropertySet.create()));

        assertThat(syncProvider.isOutOfSync()).isTrue();
    }

    @Test
    public void shouldNotBeOutOfSyncIfThereAreNoLocalChanges() {
        when(stationsStorage.getRecentStationsToSync()).thenReturn(Collections.<PropertySet>emptyList());

        assertThat(syncProvider.isOutOfSync()).isFalse();
    }

    @Test
    public void shouldNotUsePeriodicSync() {
        assertThat(syncProvider.usePeriodicSync()).isFalse();
        assertThat(syncProvider.staleTime()).isEqualTo(TimeUnit.HOURS.toMillis(24));
    }
}
