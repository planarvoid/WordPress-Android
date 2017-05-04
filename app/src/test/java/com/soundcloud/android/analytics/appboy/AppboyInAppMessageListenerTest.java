package com.soundcloud.android.analytics.appboy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.appboy.models.IInAppMessage;
import com.appboy.ui.inappmessage.InAppMessageOperation;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class AppboyInAppMessageListenerTest extends AndroidUnitTest {

    @Mock FeatureFlags featureFlags;
    @Mock IInAppMessage iInAppMessage;

    private AppboyInAppMessageListener listener;

    @Before
    public void setUp() {
       listener = new AppboyInAppMessageListener(featureFlags);
    }

    @Test
    public void discardsMessageIfDisplayPrestitialFeatureFlagIsOn() {
        when(featureFlags.isEnabled(Flag.DISPLAY_PRESTITIAL)).thenReturn(true);

        assertThat(listener.beforeInAppMessageDisplayed(iInAppMessage)).isEqualTo(InAppMessageOperation.DISCARD);
    }

    @Test
    public void displaysMessageIfDisplayPrestitialFeatureFlagIsOff() {
        when(featureFlags.isEnabled(Flag.DISPLAY_PRESTITIAL)).thenReturn(false);

        assertThat(listener.beforeInAppMessageDisplayed(iInAppMessage)).isEqualTo(InAppMessageOperation.DISPLAY_NOW);
    }
}