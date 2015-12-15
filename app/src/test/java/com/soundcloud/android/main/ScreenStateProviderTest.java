package com.soundcloud.android.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class ScreenStateProviderTest extends AndroidUnitTest {
    @Mock private AppCompatActivity activity;

    private ScreenStateProvider lightCycle;

    @Before
    public void setUp() throws Exception {
        lightCycle = new ScreenStateProvider();
    }

    @Test
    public void isNotForegroundByDefault() {
        assertThat(lightCycle.isForeground()).isFalse();
    }

    @Test
    public void isForegroundWhenOnResume() {
        lightCycle.onResume(activity);
        assertThat(lightCycle.isForeground()).isTrue();
    }

    @Test
    public void isNotForegroundOnPause() {
        lightCycle.onResume(activity);
        lightCycle.onPause(activity);
        assertThat(lightCycle.isForeground()).isFalse();
    }

    @Test
    public void isNotConfigurationChangeWhenStarting() {
        lightCycle.onCreate(activity, null);
        lightCycle.onResume(activity);
        assertThat(lightCycle.isConfigurationChange()).isFalse();
    }

    @Test
    public void isConfigurationChangeWhenScreenRotation() {
        when(activity.getChangingConfigurations()).thenReturn(Configuration.ORIENTATION_LANDSCAPE);

        final Bundle bundle = new Bundle();
        lightCycle.onCreate(activity, null);
        lightCycle.onResume(activity);
        lightCycle.onPause(activity);
        lightCycle.onSaveInstanceState(activity, bundle);
        lightCycle.onCreate(activity, bundle);
        lightCycle.onResume(activity);

        assertThat(lightCycle.isConfigurationChange()).isTrue();
    }

    @Test
    public void isNotConfigurationChangeConfigurationIsUnset() {
        when(activity.getChangingConfigurations()).thenReturn(0);

        final Bundle bundle = new Bundle();
        lightCycle.onCreate(activity, null);
        lightCycle.onResume(activity);
        lightCycle.onPause(activity);
        lightCycle.onSaveInstanceState(activity, bundle);
        lightCycle.onCreate(activity, bundle);
        lightCycle.onResume(activity);

        assertThat(lightCycle.isConfigurationChange()).isFalse();
    }
}
