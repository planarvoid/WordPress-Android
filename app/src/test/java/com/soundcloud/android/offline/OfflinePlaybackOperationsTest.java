package com.soundcloud.android.offline;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

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
    public void isOfflinePlaybackModeWhenFeatureOnAndOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        expect(operations.isOfflinePlaybackMode()).toBeTrue();
    }

    @Test
    public void isNotOfflinePlaybackModeWhenFeatureOff() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        expect(operations.isOfflinePlaybackMode()).toBeFalse();
    }

}
