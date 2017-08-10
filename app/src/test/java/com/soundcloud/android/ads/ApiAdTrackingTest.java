package com.soundcloud.android.ads;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiAdTrackingTest {

    @Test
    public void replacesNullValueWithEmptyList() {
        ApiAdTracking adTracking = ApiAdTracking.create(null, null, null, null, null, null, null,
                                                        null, null, null, null, null, null, null);
        assertThat(adTracking.clickUrls()).isEmpty();
    }
}
