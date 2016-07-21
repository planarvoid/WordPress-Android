package com.soundcloud.android.sync.posts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.sync.Syncable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Provider;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class PlaylistPostsSyncProviderTest {

    private PlaylistPostsSyncProvider syncProvider;

    @Mock private Provider<PostsSyncer> postsSyncerProvider;
    @Mock private PlaylistStorage playlistStorage;

    @Before
    public void setUp() {
        this.syncProvider = new PlaylistPostsSyncProvider(postsSyncerProvider, playlistStorage);
    }

    @Test
    public void shouldSyncCorrectEntityType() {
        assertThat(syncProvider.id()).isEqualTo(Syncable.PLAYLIST_POSTS.name());
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
