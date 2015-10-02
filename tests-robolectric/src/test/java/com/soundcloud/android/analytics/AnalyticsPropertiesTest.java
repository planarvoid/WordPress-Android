package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsPropertiesTest {

    @Mock
    private Resources resources;

    @Test
    public void shouldSpecifyAnalyticsAvailable(){
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(true);
        expect(new AnalyticsProperties(resources).isAnalyticsAvailable()).toBeTrue();
    }
}
