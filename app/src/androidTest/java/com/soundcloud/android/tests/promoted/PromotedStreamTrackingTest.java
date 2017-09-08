package com.soundcloud.android.tests.promoted;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.soundcloud.android.framework.TestUser.playerUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableFacebookInvitesNotification;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class PromotedStreamTrackingTest extends ActivityTest<MainActivity> {

    private static final String PROMOTED_PLAY = "specs/promoted-play.spec";
    private static final String PROMOTED_BY_PLAY = "specs/promoted-by-play.spec";

    public PromotedStreamTrackingTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return playerUser;
    }

    @Override
    public void setUp() throws Exception {
        disableFacebookInvitesNotification(getInstrumentation().getTargetContext());
        super.setUp();
    }

    @Test
    public void testPlayPromotedTrackFromStream() throws Exception {
        StreamScreen streamScreen = mainNavHelper.goToStream();

        // do not run when promoted item is a promoted playlist
        if (streamScreen.isFirstTrackCardPromoted()) {
            final boolean hasPromoter = streamScreen.isPromotedTrackCardWithPromoter();

            VisualPlayerElement playerElement = streamScreen.clickFirstTrackCard();
            assertThat(playerElement, is(visible()));
            assertThat(playerElement, is(playing()));

            if (hasPromoter) {
                mrLocalLocal.verify(PROMOTED_BY_PLAY);
            } else {
                mrLocalLocal.verify(PROMOTED_PLAY);
            }
        }
    }

}
