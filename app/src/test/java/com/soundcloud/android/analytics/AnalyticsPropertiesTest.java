package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.SharedPreferences;
import android.content.res.Resources;

@RunWith(SoundCloudTestRunner.class)
public class AnalyticsPropertiesTest {

    private AnalyticsProperties analyticsProperties;
    @Mock
    private Resources resources;
    @Mock
    private SharedPreferences sharedPreferences;

    @Before
    public void setUp() throws Exception {
        when(resources.getString(R.string.localytics_app_key)).thenReturn("localyticsKey");
    }

    @Test
    public void shouldRetrieveLocalyticsKeyIfAnalyticsIsEnabled(){
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(true);
        analyticsProperties = new AnalyticsProperties(resources, sharedPreferences);
        expect(analyticsProperties.getLocalyticsAppKey()).toBe("localyticsKey");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfAnalyticsEnabledAndLocalyticsKeyIsEmpty(){
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(true);
        when(resources.getString(R.string.localytics_app_key)).thenReturn("");
        analyticsProperties = new AnalyticsProperties(resources, sharedPreferences);
    }

    @Test
    public void shouldSpecifyAnalyticsAvailable(){
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(true);
        expect(new AnalyticsProperties(resources, sharedPreferences).isAnalyticsAvailable()).toBeTrue();
    }
}
