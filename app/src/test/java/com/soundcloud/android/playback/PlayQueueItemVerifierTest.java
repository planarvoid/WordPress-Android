package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.TrackOfflineStateProvider;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PlayQueueItemVerifierTest {

    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private TrackOfflineStateProvider trackOfflineStateProvider;


    private PlayQueueItemVerifier playQueueItemVerifier;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        playQueueItemVerifier = new PlayQueueItemVerifier(networkConnectionHelper, trackOfflineStateProvider);
    }

    @Test
    public void shouldReturnTrueIfTrack() {
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        PlayQueueItem playQueueItem = TestPlayQueueItem.createTrack(Urn.NOT_SET);

        assertThat(playQueueItemVerifier.isItemPlayable(playQueueItem)).isTrue();
    }

    @Test
    public void shouldReturnFalseIfBlocked() {
        PlayQueueItem playQueueItem = TestPlayQueueItem.createBlockedTrack(Urn.NOT_SET);

        assertThat(playQueueItemVerifier.isItemPlayable(playQueueItem)).isFalse();
    }

    @Test
    public void shouldReturnFalseIfPlayList() {
        PlayQueueItem playQueueItem = TestPlayQueueItem.createPlaylist(Urn.NOT_SET);

        assertThat(playQueueItemVerifier.isItemPlayable(playQueueItem)).isFalse();
    }

    @Test
    public void shouldReturnTrueIfVideoAd() {
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        PlayQueueItem playQueueItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.NOT_SET));

        assertThat(playQueueItemVerifier.isItemPlayable(playQueueItem)).isTrue();
    }

    @Test
    public void shouldReturnTrueIfOfflineAndDownloaded() {
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(false);
        when(trackOfflineStateProvider.getOfflineState(any())).thenReturn(OfflineState.DOWNLOADED);

        PlayQueueItem playQueueItem = TestPlayQueueItem.createTrack(Urn.NOT_SET);

        assertThat(playQueueItemVerifier.isItemPlayable(playQueueItem)).isTrue();
    }

    @Test
    public void shouldReturnFalseIfOfflineAndNotDownloaded() {
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(false);
        when(trackOfflineStateProvider.getOfflineState(any())).thenReturn(OfflineState.NOT_OFFLINE);

        PlayQueueItem playQueueItem = TestPlayQueueItem.createTrack(Urn.NOT_SET);

        assertThat(playQueueItemVerifier.isItemPlayable(playQueueItem)).isFalse();
    }

}
