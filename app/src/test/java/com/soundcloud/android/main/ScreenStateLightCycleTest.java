package com.soundcloud.android.main;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class ScreenStateLightCycleTest {
    @Mock private FragmentActivity activity;
    
    private ScreenStateLightCycle lightCycle;

    @Before
    public void setUp() throws Exception {
        lightCycle = new ScreenStateLightCycle();
    }

    @Test
    public void isNotForegroundByDefault() {
        expect(lightCycle.isForeground()).toBeFalse();
    }

    @Test
    public void isForegroundWhenOnResume() {
        lightCycle.onResume(activity);
        expect(lightCycle.isForeground()).toBeTrue();
    }

    @Test
    public void isNotForegroundOnPause() {
        lightCycle.onResume(activity);
        lightCycle.onPause(activity);
        expect(lightCycle.isForeground()).toBeFalse();
    }

    @Test
    public void isNotConfigurationChangeWhenStarting() {
        lightCycle.onCreate(activity, null);
        lightCycle.onResume(activity);
        expect(lightCycle.isConfigurationChange()).toBeFalse();
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

        expect(lightCycle.isConfigurationChange()).toBeTrue();
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

        expect(lightCycle.isConfigurationChange()).toBeFalse();
    }
}