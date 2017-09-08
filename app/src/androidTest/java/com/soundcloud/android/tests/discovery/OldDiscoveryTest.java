package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.properties.Flag.DISCOVER_BACKEND;
import static com.soundcloud.android.properties.Flag.NEW_FOR_YOU_FIRST;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.discovery.OldDiscoveryScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OldDiscoveryTest extends ActivityTest<MainActivity> {

    private OldDiscoveryScreen discoveryScreen;

    public OldDiscoveryTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.goToOldDiscovery();
    }

    @Override
    protected void beforeActivityLaunched() {
        getFeatureFlags().enable(NEW_FOR_YOU_FIRST);
        getFeatureFlags().disable(DISCOVER_BACKEND);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        getFeatureFlags().reset(DISCOVER_BACKEND);
        getFeatureFlags().reset(NEW_FOR_YOU_FIRST);
        super.tearDown();
    }

    @Test
    public void testNewForYouIsVisible() throws Exception {
        assertThat(discoveryScreen.newForYouBucket().isOnScreen()).isTrue();
    }
}
