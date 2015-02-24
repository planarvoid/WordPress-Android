package com.soundcloud.android.offline;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class OfflinePlaybackOperationsTest {

    @Mock private FeatureOperations featureOperations;
    @Mock private NetworkConnectionHelper connectionHelper;

    private OfflinePlaybackOperations operations;

    @Before
    public void setUp() throws Exception {
        operations = new OfflinePlaybackOperations(featureOperations, connectionHelper);
    }

    @Test
    public void shouldCreateOfflinePlayQueueWhenFeatureEnabledAndOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        expect(operations.shouldCreateOfflinePlayQueue()).toBeTrue();
    }

    @Test
    public void shouldNotCreateOfflinePlayQueueWhenFeatureDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        expect(operations.shouldCreateOfflinePlayQueue()).toBeFalse();
    }

    @Test
    public void shouldPlayOfflineWhenFeatureEnabledAndTrackDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        expect(operations.shouldPlayOffline(downloadedTrack())).toBeTrue();
    }

    @Test
    public void shouldNotPlayOfflineWhenFeatureEnabledAndTrackRemoved() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        expect(operations.shouldPlayOffline(removedTrack())).toBeFalse();
    }

    @Test
    public void shouldNotPlayOfflineWhenFeatureEnabledAndTrackNotDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        expect(operations.shouldPlayOffline(notDownloadedTrack())).toBeFalse();
    }

    @Test
    public void shouldNotPlayOfflineWhenFeatureDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        expect(operations.shouldPlayOffline(downloadedTrack())).toBeFalse();
    }

    private PropertySet downloadedTrack() {
        return PropertySet.from(TrackProperty.OFFLINE_DOWNLOADED_AT.bind(new Date()));
    }

    private PropertySet removedTrack() {
        return PropertySet.from(
                TrackProperty.OFFLINE_DOWNLOADED_AT.bind(new Date()),
                TrackProperty.OFFLINE_REMOVED_AT.bind(new Date()));
    }

    private PropertySet notDownloadedTrack() {
        return PropertySet.create();
    }

}
