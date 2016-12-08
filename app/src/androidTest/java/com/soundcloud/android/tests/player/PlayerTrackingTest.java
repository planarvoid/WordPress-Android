package com.soundcloud.android.tests.player;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class PlayerTrackingTest extends TrackingActivityTest<MainActivity> {
    private static final String TEST_SCENARIO_EXPAND_COLLAPSE = "player-expand-collapse";
    private StreamScreen streamScreen;

    public PlayerTrackingTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.playerUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        streamScreen = new StreamScreen(solo);
    }

    public void testTrackPlayerExpandAndCollapse() {
        startEventTracking();

        final VisualPlayerElement player = streamScreen.clickFirstNotPromotedTrackCard()
                                                       .waitForExpandedPlayer();

        player.pressCloseButton();
        player.waitForCollapsedPlayer();

        finishEventTracking(TEST_SCENARIO_EXPAND_COLLAPSE);
    }

}
