package com.soundcloud.android.ads;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ApiAdTrackingTest {

    @Test
    public void replacesNullValueWithEmptyList() {
        ApiAdTracking adTracking = new ApiAdTracking(null, null, null, null, null, null, null,
                                                     null, null, null, null, null, null, null);
        assertThat(adTracking.clickUrls).isEmpty();
    }
}
