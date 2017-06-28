package com.soundcloud.android.tests.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.discovery.OldDiscoveryScreen;
import com.soundcloud.android.tests.ActivityTest;

public class OldDiscoveryTest extends ActivityTest<MainActivity> {

    private OldDiscoveryScreen discoveryScreen;

    public OldDiscoveryTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.defaultUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.goToOldDiscovery();
    }

    @Override
    protected void beforeStartActivity() {
        FeatureFlagsHelper.create(getInstrumentation().getTargetContext()).enable(Flag.NEW_FOR_YOU_FIRST);
    }

    public void testNewForYouIsVisible() throws Exception {
        assertThat(discoveryScreen.newForYouBucket().isOnScreen()).isTrue();
    }
}
