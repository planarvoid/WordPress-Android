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

    @Mock private Resources resources;

    private FeatureFlags featureFlags;

    @Before
    public void setUp() throws Exception {
        featureFlags = new FeatureFlags(resources);
    }

    @Test
    public void shouldBeEnabledWhenResourceValueIsTrue() {
        when(resources.getBoolean(anyInt())).thenReturn(true);

        expect(featureFlags.isEnabled(Flag.TEST_FEATURE)).toBeTrue();
        expect(featureFlags.isDisabled(Flag.TEST_FEATURE)).toBeFalse();
    }

    @Test
    public void shouldBeDisabledWhenResourceValueIsFalse() {
        when(resources.getBoolean(anyInt())).thenReturn(false);

        expect(featureFlags.isEnabled(Flag.TEST_FEATURE)).toBeFalse();
        expect(featureFlags.isDisabled(Flag.TEST_FEATURE)).toBeTrue();
    }

}