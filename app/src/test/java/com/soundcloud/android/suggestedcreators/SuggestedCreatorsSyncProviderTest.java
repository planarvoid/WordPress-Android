package com.soundcloud.android.suggestedcreators;

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
public class SuggestedCreatorsSyncProviderTest {

    private SuggestedCreatorsSyncProvider syncProvider;

    @Mock private Provider<SuggestedCreatorsSyncer> suggestedCreatorsSyncerProvider;

    @Before
    public void setUp() {
        this.syncProvider = new SuggestedCreatorsSyncProvider(suggestedCreatorsSyncerProvider);
    }

    @Test
    public void shouldSyncCorrectEntityType() {
        assertThat(syncProvider.id()).isEqualTo(Syncable.SUGGESTED_CREATORS.name());
    }

    @Test
    public void shouldNeverSync() {
        assertThat(syncProvider.usePeriodicSync()).isFalse();
        assertThat(syncProvider.staleTime()).isEqualTo(TimeUnit.DAYS.toMillis(1));
        assertThat(syncProvider.isOutOfSync()).isFalse();
    }
}
