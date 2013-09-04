package com.soundcloud.android.analytics;

import android.content.res.Resources;
import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsPropertiesTest {

    private AnalyticsProperties analyticsProperties;
    @Mock
    private Resources resources;

    @Test
    public void shouldRetrieveLocalyticsKeyIfAnalyticsIsEnabled(){
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(true);
        when(resources.getString(R.string.localytics_app_key)).thenReturn("localyticsKey");
        analyticsProperties = new AnalyticsProperties(resources);
        expect(analyticsProperties.getLocalyticsAppKey()).toBe("localyticsKey");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfAnalyticsEnabledAndLocalyticsKeyIsEmpty(){
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(true);
        when(resources.getString(R.string.localytics_app_key)).thenReturn("");
        analyticsProperties = new AnalyticsProperties(resources);
    }

    @Test
    public void shouldSpecifyAnalyticsDisabled(){
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(false);
        when(resources.getString(R.string.localytics_app_key)).thenReturn("localyticsKey");
        expect(new AnalyticsProperties(resources).isAnalyticsDisabled()).toBeTrue();
    }


}
