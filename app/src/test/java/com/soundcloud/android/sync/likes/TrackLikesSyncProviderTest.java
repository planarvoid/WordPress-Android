package com.soundcloud.android.sync.likes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.sync.Syncable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Provider;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class TrackLikesSyncProviderTest {

    private TrackLikesSyncProvider syncProvider;

    @Mock private Provider<LikesSyncer<ApiTrack>> trackLikesSyncer;
    @Mock private MyTrackLikesStateProvider myTrackLikesStateProvider;

    @Before
    public void setUp() {
        this.syncProvider = new TrackLikesSyncProvider(trackLikesSyncer, myTrackLikesStateProvider);
    }

    @Test
    public void shouldSyncCorrectEntityType() {
        assertThat(syncProvider.id()).isEqualTo(Syncable.TRACK_LIKES.name());
    }

    @Test
    public void shouldBeOutOfSyncIfThereAreLocalChanges() {
        when(myTrackLikesStateProvider.hasLocalChanges()).thenReturn(true);

        assertThat(syncProvider.isOutOfSync()).isTrue();
    }

    @Test
    public void shouldNotBeOutOfSyncIfThereAreNoLocalChanges() {
        when(myTrackLikesStateProvider.hasLocalChanges()).thenReturn(false);

        assertThat(syncProvider.isOutOfSync()).isFalse();
    }

    @Test
    public void shouldUsePeriodicSync() {
        assertThat(syncProvider.usePeriodicSync()).isTrue();
        assertThat(syncProvider.staleTime()).isEqualTo(TimeUnit.HOURS.toMillis(1));
    }
}
