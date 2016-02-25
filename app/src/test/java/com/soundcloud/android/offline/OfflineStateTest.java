package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineState.getOfflineState;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class OfflineStateTest {

    @Test
    public void requestedStateIfAny() {
        assertThat(getOfflineState(true, true, true)).isEqualTo(OfflineState.REQUESTED);
    }

    @Test
    public void downloadedStateIfAnyWhenNoRequested() {
        assertThat(getOfflineState(false, true, true)).isEqualTo(OfflineState.DOWNLOADED);
    }

    @Test
    public void unavailableStateIfAnyWhenNoRequestedOrDownloaded() {
        assertThat(getOfflineState(false, false, true)).isEqualTo(OfflineState.UNAVAILABLE);
    }

    @Test
    public void requestedByDefault() {
        // Likely to happen for empty playlists.
        assertThat(getOfflineState(false, false, false)).isEqualTo(OfflineState.REQUESTED);
    }
}
