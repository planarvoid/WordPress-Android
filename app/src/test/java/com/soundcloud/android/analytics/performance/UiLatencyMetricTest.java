package com.soundcloud.android.analytics.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.onboarding.OnboardActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import android.app.Activity;
import android.content.ComponentName;

@RunWith(MockitoJUnitRunner.class)
public class UiLatencyMetricTest {

    @Test
    public void shouldReturnCorrectTypesFromActivityCreation() {
        assertThat(UiLatencyMetric.fromActivity(mockActivity(MainActivity.class))).isEqualTo(UiLatencyMetric.MAIN_AUTHENTICATED);
        assertThat(UiLatencyMetric.fromActivity(mockActivity(OnboardActivity.class))).isEqualTo(UiLatencyMetric.UNAUTHENTICATED);
        assertThat(UiLatencyMetric.fromActivity(mockActivity(SearchActivity.class))).isEqualTo(UiLatencyMetric.NONE);
    }

    private Activity mockActivity(Class<? extends Activity> activityClass) {
        final Activity activity = mock(Activity.class);
        final ComponentName componentName = mock(ComponentName.class);
        when(componentName.getClassName()).thenReturn(activityClass.getName());
        when(activity.getComponentName()).thenReturn(componentName);

        return activity;
    }
}
