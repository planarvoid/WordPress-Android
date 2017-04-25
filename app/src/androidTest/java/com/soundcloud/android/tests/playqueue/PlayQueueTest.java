package com.soundcloud.android.tests.playqueue;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class PlayQueueTest extends ActivityTest<MainActivity> {
    private static final String TEST_SCENARIO_PLAY_QUEUE = "specs/play-queue.spec";
    private VisualPlayerElement player;

    public PlayQueueTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.playerUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        StreamScreen streamScreen = new StreamScreen(solo);
        player = streamScreen.clickFirstNotPromotedTrackCard()
                             .waitForExpandedPlayer();
    }

    public void testPlayQueueExpandAndCollapse() throws Exception {
        mrLocalLocal.startEventTracking();

        player.pressPlayQueueButton()
              .pressShuffleButton()
              .pressRepeatButton()
              .pressRepeatButton()
              .pressRepeatButton()
              .pressCloseButton();

        mrLocalLocal.verify(TEST_SCENARIO_PLAY_QUEUE);
    }

}
