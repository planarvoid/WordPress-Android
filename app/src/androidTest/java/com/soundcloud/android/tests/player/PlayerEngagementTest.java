package com.soundcloud.android.tests.player;

import static com.soundcloud.android.framework.TestUser.engagementsUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static com.soundcloud.android.framework.matcher.view.IsEnabled.Enabled;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.PlayerMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class PlayerEngagementTest extends TrackingActivityTest<LauncherActivity> {
    private static final String REPOST_TRACK_PLAYING_FROM_STREAM = "stream_engagements_repost_from_player";
    private static final String LIKE_TRACK_PLAYING_FROM_STREAM = "stream_engagements_like_from_player";

    private StreamScreen streamScreen;

    public PlayerEngagementTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return engagementsUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        streamScreen = new StreamScreen(solo);
    }

    public void testPlayAndPauseTrackFromStream() {
        startEventTracking();

        final VisualPlayerElement playerElement =
                streamScreen.clickFirstNotPromotedTrackCard();

        assertThat(playerElement, is(expanded()));
        assertThat(playerElement, is(visible()));
        assertThat(playerElement, is(playing()));

        playerElement.clickArtwork();

        assertThat(playerElement, is(not(playing())));

        // Like
        ViewElement likeButton = playerElement.likeButton();
        likeButton.click();
        likeButton.click();

        finishEventTracking(LIKE_TRACK_PLAYING_FROM_STREAM);

        playerElement.pressBackToCollapse();

        startEventTracking();

        final VisualPlayerElement playerElement2 =
                streamScreen.clickFirstNotPromotedTrackCard();

        // Repost
        playerElement2.clickMenu().toggleRepost();
        // fake wait, so that the snackbar with reposted message disappears
        playerElement2.playForFiveSeconds();
        playerElement2.clickMenu().toggleRepost();

        finishEventTracking(REPOST_TRACK_PLAYING_FROM_STREAM);

        PlayerMenuElement menu = playerElement2.clickMenu();
        assertThat(menu.repostItem(), is(Enabled()));
        assertThat(menu.shareItem(), is(Enabled()));
    }
}
