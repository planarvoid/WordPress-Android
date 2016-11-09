package com.soundcloud.android.discovery.recommendedplaylists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.sync.Syncable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Provider;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class RecommendedPlaylistsSyncProviderTest {

    private RecommendedPlaylistsSyncProvider syncProvider;

    @Mock private Provider<RecommendPlaylistsSyncer> recommendPlaylistsSyncerProvider;

    @Before
    public void setUp() {
        this.syncProvider = new RecommendedPlaylistsSyncProvider(recommendPlaylistsSyncerProvider);
    }

    @Test
    public void shouldSyncCorrectEntityType() {
        assertThat(syncProvider.id()).isEqualTo(Syncable.RECOMMENDED_PLAYLISTS.name());
    }

    @Test
    public void shouldNeverSync() {
        assertThat(syncProvider.usePeriodicSync()).isFalse();
        assertThat(syncProvider.staleTime()).isEqualTo(TimeUnit.DAYS.toMillis(1));
        assertThat(syncProvider.isOutOfSync()).isFalse();
    }
}
