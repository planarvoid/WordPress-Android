package com.soundcloud.android.properties;

import android.content.res.Resources;
import com.soundcloud.android.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsPropertiesTest {

    private AnalyticsProperties analyticsProperties;
    @Mock
    private Resources resources;

    @Test
    public void shouldNotRetrieveLocalyticsKeyIfAnalyticsIsNotEnabled(){
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(false);
        analyticsProperties = new AnalyticsProperties(resources);
        verify(resources, never()).getString(anyInt());
    }


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

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenAskingForLocalyticsKeyIfAnalyticsNotEnabled(){
        new AnalyticsProperties(resources).getLocalyticsAppKey();
    }

    @Test
    public void shouldSpecifyAnalyticsDisabled(){
        expect(new AnalyticsProperties(resources).isAnalyticsDisabled()).toBeTrue();
    }


}
