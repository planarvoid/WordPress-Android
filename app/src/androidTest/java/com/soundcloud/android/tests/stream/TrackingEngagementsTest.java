package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.helpers.MainNavigationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.StreamCardElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

@EventTrackingTest
public class TrackingEngagementsTest extends TrackingActivityTest<MainActivity> {

    private static final String REPOST_ENGAGEMENTS_FROM_STREAM = "stream_engagements_repost_scenario";
    private static final String REPOST_TRACK_PLAYING_FROM_STREAM = "stream_engagements_repost_from_player";
    private static final String LIKE_ENGAGEMENTS_FROM_STREAM = "stream_engagements_like_scenario";
    private static final String LIKE_TRACK_PLAYING_FROM_STREAM = "stream_engagements_like_from_player";

    private StreamScreen streamScreen;

    public TrackingEngagementsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        streamUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.NEW_STREAM);

        super.setUp();
        mainNavHelper.goToStream();
        streamScreen = new StreamScreen(solo);
    }

    public void testRepostEngagementsTracking() {
        startEventTracking();

        StreamCardElement trackCard = streamScreen.firstNotPromotedTrackCard();

        // toggle from card item
        boolean initRepostState = trackCard.isReposted();
        trackCard.toggleRepost();
        assertThat(trackCard.isReposted(), is(!initRepostState));

        // toggle from overflow
        TrackItemMenuElement menuElement = trackCard.clickOverflowButton();
        menuElement.toggleRepost();

        finishEventTracking(REPOST_ENGAGEMENTS_FROM_STREAM);
    }

    public void testLikeEngagementsTracking() {
        startEventTracking();

        StreamCardElement trackCard = streamScreen.firstNotPromotedTrackCard();

        TrackItemMenuElement menuElement = trackCard.clickOverflowButton();
        boolean initLikeStatus = menuElement.isLiked();
        menuElement.toggleLike();

        trackCard.toggleLike();
        assertThat(trackCard.isLiked(), is(initLikeStatus));

        finishEventTracking(LIKE_ENGAGEMENTS_FROM_STREAM);
    }

    public void testLikePlayingTrackFromStream() {
        startEventTracking();

        VisualPlayerElement visualPlayerElement = streamScreen.firstNotPromotedTrackCard().click();
        assertTrue(visualPlayerElement.isExpandedPlayerPlaying());

        ViewElement likeButton = visualPlayerElement.likeButton();
        // toggle
        likeButton.click();

        // and untoggle
        likeButton.click();

        finishEventTracking(LIKE_TRACK_PLAYING_FROM_STREAM);
    }

    public void testRepostPlayingTrackFromStream() {
        startEventTracking();

        VisualPlayerElement visualPlayerElement = streamScreen.firstNotPromotedTrackCard().click();
        assertTrue(visualPlayerElement.isExpandedPlayerPlaying());

        // toggle
        visualPlayerElement.clickMenu().toggleRepost();

        // and untoggle
        visualPlayerElement.clickMenu().toggleRepost();

        finishEventTracking(REPOST_TRACK_PLAYING_FROM_STREAM);
    }

}
