package com.soundcloud.android.properties;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;

@RunWith(SoundCloudTestRunner.class)
public class FeatureFlagsTest {

    @Mock
    private Resources resources;

    private FeatureFlags featureFlags;

    @Before
    public void setUp() throws Exception {
        featureFlags = new FeatureFlags(resources);
    }

    @Test
    public void shouldBeEnabledWhenResourceValueIsTrue() {
        when(resources.getBoolean(anyInt())).thenReturn(true);

        expect(featureFlags.isEnabled(Feature.NEW_STREAM)).toBeTrue();
        expect(featureFlags.isDisabled(Feature.NEW_STREAM)).toBeFalse();
    }

    @Test
    public void shouldBeDisabledWhenResourceValueIsFalse() {
        when(resources.getBoolean(anyInt())).thenReturn(false);

        expect(featureFlags.isEnabled(Feature.NEW_STREAM)).toBeFalse();
        expect(featureFlags.isDisabled(Feature.NEW_STREAM)).toBeTrue();
    }

}