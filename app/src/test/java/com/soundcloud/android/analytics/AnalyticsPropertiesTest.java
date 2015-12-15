package com.soundcloud.android.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.res.Resources;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsPropertiesTest {

    @Mock private Resources resources;

    @Test
    public void shouldSpecifyAnalyticsAvailable(){
        when(resources.getBoolean(R.bool.analytics_enabled)).thenReturn(true);
        assertThat(new AnalyticsProperties(resources).isAnalyticsAvailable()).isTrue();
    }
}
