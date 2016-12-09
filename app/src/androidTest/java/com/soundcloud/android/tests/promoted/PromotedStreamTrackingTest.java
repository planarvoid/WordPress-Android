package com.soundcloud.android.tests.promoted;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class PromotedStreamTrackingTest extends TrackingActivityTest<MainActivity> {

    private static final String PROMOTED_PLAY = "promoted-play";
    private static final String PROMOTED_BY_PLAY = "promoted-by-play";

    public PromotedStreamTrackingTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.playerUser;
    }

    @Override
    protected void setUp() throws Exception {
        ConfigurationHelper.disableFacebookInvitesNotification(getInstrumentation().getTargetContext());
        super.setUp();
    }

    public void testPlayPromotedTrackFromStream() {
        startEventTracking();
        StreamScreen streamScreen = mainNavHelper.goToStream();

        // do not run when promoted item is a promoted playlist
        if (streamScreen.isFirstTrackCardPromoted()) {
            final boolean hasPromoter = streamScreen.isPromotedTrackCardWithPromoter();

            VisualPlayerElement playerElement = streamScreen.clickFirstTrackCard();
            assertThat(playerElement, is(visible()));
            assertThat(playerElement, is(playing()));

            finishEventTracking(hasPromoter ? PROMOTED_BY_PLAY : PROMOTED_PLAY);
        }
    }

}
