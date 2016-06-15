package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.net.Uri;
import android.os.Bundle;

public class LegacySyncContentTest extends AndroidUnitTest {

    private static final LegacySyncContent MY_SOUNDS = LegacySyncContent.MySounds;
    private static final Uri URI = MY_SOUNDS.content.uri;
    private static final long DELAY = MY_SOUNDS.syncDelay;

    @Mock SyncStateManager syncStateManager;

    private Bundle syncResult;

    @Before
    public void setUp() throws Exception {
        syncResult = new Bundle();
    }

    @Test
    public void shouldResetSyncMisses() {
        syncResult.putBoolean(URI.toString(), true);

        LegacySyncContent.updateCollections(syncStateManager, syncResult);

        verify(syncStateManager).resetSyncMisses(URI);
    }

    @Test
    public void shouldIncrementSyncMisses() {
        syncResult.putBoolean(URI.toString(), false);

        LegacySyncContent.updateCollections(syncStateManager, syncResult);

        verify(syncStateManager).incrementSyncMiss(URI);
    }

    @Test
    public void shouldNotSyncWhenInDelay() {
        long lastSync = System.currentTimeMillis();

        assertThat(MY_SOUNDS.shouldSync(0, lastSync)).isFalse();
    }

    @Test
    public void shouldSyncWhenOutDelay() {
        long lastSync = System.currentTimeMillis() - DELAY;

        assertThat(MY_SOUNDS.shouldSync(0, lastSync)).isTrue();
    }

    @Test
    public void shouldNotSyncWhenInBackoff() {
        long lastSync = System.currentTimeMillis() - DELAY;

        assertThat(MY_SOUNDS.shouldSync(1, lastSync)).isFalse();
    }

    @Test
    public void shouldSyncWhenOutBackoff() {
        int backoffMultiplier = MY_SOUNDS.backoffMultipliers[1];
        long lastSync = System.currentTimeMillis() - (DELAY * backoffMultiplier);

        assertThat(MY_SOUNDS.shouldSync(1, lastSync)).isTrue();
    }

}
