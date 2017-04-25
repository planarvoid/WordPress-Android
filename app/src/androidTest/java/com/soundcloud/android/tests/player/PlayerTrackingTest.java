package com.soundcloud.android.tests.player;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class PlayerTrackingTest extends ActivityTest<MainActivity> {
    private static final String TEST_SCENARIO_EXPAND_COLLAPSE = "specs/player-expand-collapse.spec";
    private StreamScreen streamScreen;

    public PlayerTrackingTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.playerUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        streamScreen = new StreamScreen(solo);
    }

    public void testTrackPlayerExpandAndCollapse() throws Exception {
        mrLocalLocal.startEventTracking();

        final VisualPlayerElement player = streamScreen.clickFirstRepostedTrack()
                                                       .waitForExpandedPlayer();

        player.pressCloseButton();
        player.waitForCollapsedPlayer();

        mrLocalLocal.verify(TEST_SCENARIO_EXPAND_COLLAPSE);
    }

}
