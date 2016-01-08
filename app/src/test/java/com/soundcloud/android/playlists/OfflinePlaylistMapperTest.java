package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.offline.OfflineState;
import org.junit.Before;
import org.junit.Test;

public class OfflinePlaylistMapperTest {

    private OfflinePlaylistMapper mapper;

    @Before
    public void setUp() throws Exception {
        mapper = new OfflinePlaylistMapper() {};
    }

    @Test
    public void requestedStateIfAny() {
        assertThat(mapper.getDownloadState(true, true, true)).isEqualTo(OfflineState.REQUESTED);
    }

    @Test
    public void downloadedStateIfAnyWhenNoRequested() {
        assertThat(mapper.getDownloadState(false, true, true)).isEqualTo(OfflineState.DOWNLOADED);
    }

    @Test
    public void unavailableStateIfAnyWhenNoRequestedOrDownloaded() {
        assertThat(mapper.getDownloadState(false, false, true)).isEqualTo(OfflineState.UNAVAILABLE);
    }

    @Test
    public void downloadedByDefault() {
        // Likely to happen for empty playlists.
        assertThat(mapper.getDownloadState(false, false, false)).isEqualTo(OfflineState.DOWNLOADED);
    }
}
