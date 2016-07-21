package com.soundcloud.android.sync.posts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Provider;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class TrackPostsSyncProviderTest {

    private TrackPostsSyncProvider syncProvider;

    @Mock private Provider<PostsSyncer> postsSyncerProvider;

    @Before
    public void setUp() {
        this.syncProvider = new TrackPostsSyncProvider(postsSyncerProvider);
    }

    @Test
    public void shouldAlwaysSync() {
        assertThat(syncProvider.usePeriodicSync()).isTrue();
        assertThat(syncProvider.staleTime()).isEqualTo(TimeUnit.HOURS.toMillis(1));
        assertThat(syncProvider.isOutOfSync()).isFalse();
    }
}
