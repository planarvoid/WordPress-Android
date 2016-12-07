package com.soundcloud.android.tests.playqueue;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.PlayQueue;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

@PlayQueue
public class PlayQueueTest extends TrackingActivityTest<MainActivity> {
    private static final String TEST_SCENARIO_PLAY_QUEUE = "play-queue";
    private VisualPlayerElement player;

    public PlayQueueTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.playerUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        FeatureFlagsHelper.create(getInstrumentation().getTargetContext()).enable(Flag.PLAY_QUEUE);
        StreamScreen streamScreen = new StreamScreen(solo);
        player = streamScreen.clickFirstNotPromotedTrackCard()
                             .waitForExpandedPlayer();
    }

    public void testPlayQueueExpandAndCollapse() {
        startEventTracking();

        player.pressPlayQueueButton()
              .pressShuffleButton()
              .pressRepeatButton()
              .pressRepeatButton()
              .pressRepeatButton()
              .pressCloseButton();

        finishEventTracking(TEST_SCENARIO_PLAY_QUEUE);
    }

}
