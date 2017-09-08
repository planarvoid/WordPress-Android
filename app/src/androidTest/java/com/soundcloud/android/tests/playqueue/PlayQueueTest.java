package com.soundcloud.android.tests.playqueue;

import static com.soundcloud.android.framework.TestUser.playerUser;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class PlayQueueTest extends ActivityTest<MainActivity> {
    private static final String TEST_SCENARIO_PLAY_QUEUE = "specs/play-queue.spec";
    private VisualPlayerElement player;

    public PlayQueueTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return playerUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        StreamScreen streamScreen = new StreamScreen(solo);
        player = streamScreen.clickFirstNotPromotedTrackCard()
                             .waitForExpandedPlayer();
    }

    @Test
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
