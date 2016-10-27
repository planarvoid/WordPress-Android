package com.soundcloud.android.sync.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.sync.Syncable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class MyPlaylistsSyncProviderTest {

    private MyPlaylistsSyncProvider syncProvider;

    @Mock private MyPlaylistsSyncerFactory myPlaylistsSyncer;
    @Mock private PlaylistStorage playlistStorage;

    @Before
    public void setUp() {
        this.syncProvider = new MyPlaylistsSyncProvider(myPlaylistsSyncer, playlistStorage);
    }

    @Test
    public void shouldSyncCorrectEntityType() {
        assertThat(syncProvider.id()).isEqualTo(Syncable.MY_PLAYLISTS.name());
    }

    @Test
    public void shouldBeOutOfSyncIfThereAreLocalChanges() {
        when(playlistStorage.hasLocalChanges()).thenReturn(true);

        assertThat(syncProvider.isOutOfSync()).isTrue();
    }

    @Test
    public void shouldNotBeOutOfSyncIfThereAreNoLocalChanges() {
        when(playlistStorage.hasLocalChanges()).thenReturn(false);

        assertThat(syncProvider.isOutOfSync()).isFalse();
    }

    @Test
    public void shouldUsePeriodicSync() {
        assertThat(syncProvider.usePeriodicSync()).isTrue();
        assertThat(syncProvider.staleTime()).isEqualTo(TimeUnit.HOURS.toMillis(1));
    }
}
