package com.soundcloud.android.sync.likes;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.sync.Syncable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Provider;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class PlaylistLikesSyncProviderTest {

    private PlaylistLikesSyncProvider syncProvider;

    @Mock private Provider<LikesSyncer<ApiPlaylist>> playlistLikesSyncer;
    @Mock private MyPlaylistLikesStateProvider myPlaylistLikesStateProvider;

    @Before
    public void setUp() {
        this.syncProvider = new PlaylistLikesSyncProvider(playlistLikesSyncer, myPlaylistLikesStateProvider);
    }

    @Test
    public void shouldSyncCorrectEntityType() {
        assertThat(syncProvider.id()).isEqualTo(Syncable.PLAYLIST_LIKES.name());
    }

    @Test
    public void shouldBeOutOfSyncIfThereAreLocalChanges() {
        when(myPlaylistLikesStateProvider.hasLocalChanges()).thenReturn(true);

        assertThat(syncProvider.isOutOfSync()).isTrue();
    }

    @Test
    public void shouldNotBeOutOfSyncIfThereAreNoLocalChanges() {
        when(myPlaylistLikesStateProvider.hasLocalChanges()).thenReturn(false);

        assertThat(syncProvider.isOutOfSync()).isFalse();
    }

    @Test
    public void shouldUsePeriodicSync() {
        assertThat(syncProvider.usePeriodicSync()).isTrue();
        assertThat(syncProvider.staleTime()).isEqualTo(TimeUnit.HOURS.toMillis(1));
    }

}
