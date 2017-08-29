package com.soundcloud.android.tests.player;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;

import android.net.Uri;
import android.widget.ToggleButton;

import java.util.regex.Pattern;

public class TrackQuietModeTest extends ResolveBaseTest {

    private static final String TRACK_WITH_HIDDEN_STATS_TITLE = "Track with hidden stats";
    private static final String TRACK_WITH_PUBLIC_STATS_TITLE = "Track with public stats";

    //the button text is "X count". The X is a placeholder for the like drawable and gets replace with a span
    private static final Pattern LIKE_BUTTON_WITH_COUNT = Pattern.compile("(X\\s)?\\d+$");
    private static final Pattern LIKE_BUTTON_WITHOUT_COUNT = Pattern.compile("(X\\s)?$");

    private ProfileScreen profileScreen;

    @Override
    protected Uri getUri() {
        return TestConsts.QUIET_MODE_CREATOR_PERMALINK;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        profileScreen = new ProfileScreen(solo);
    }

    public void testTrackWithPublicStatsShouldDisplayPlaysCount() {
        final boolean isPlayCountVisible = profileScreen.trackWithTitle(TRACK_WITH_PUBLIC_STATS_TITLE)
                                                        .playCount()
                                                        .hasVisibility();
        assertThat(isPlayCountVisible).isTrue();
    }

    public void testTrackWithHiddenStatsShouldNotDisplayPlaysCount() {
        final boolean isPlayCountVisible = profileScreen.trackWithTitle(TRACK_WITH_HIDDEN_STATS_TITLE)
                                                        .playCount()
                                                        .hasVisibility();
        assertThat(isPlayCountVisible).isFalse();
    }

    public void testTrackWithPublicStatsShouldDisplayLikesCountInPlayer() {
        final ToggleButton likeButton = profileScreen.playTrackWithTitle(TRACK_WITH_PUBLIC_STATS_TITLE)
                                                     .waitForExpandedPlayer()
                                                     .likeButton()
                                                     .toToggleButton();

        assertThat(likeButton.getTextOn()).containsPattern(LIKE_BUTTON_WITH_COUNT);
        assertThat(likeButton.getTextOff()).containsPattern(LIKE_BUTTON_WITH_COUNT);
    }

    public void testTrackWithHiddenStatsShouldNotDisplayLikesCountInPlayer() {
        final ToggleButton likeButton = profileScreen.playTrackWithTitle(TRACK_WITH_HIDDEN_STATS_TITLE)
                                                     .waitForExpandedPlayer()
                                                     .likeButton()
                                                     .toToggleButton();

        assertThat(likeButton.getTextOn()).containsPattern(LIKE_BUTTON_WITHOUT_COUNT);
        assertThat(likeButton.getTextOff()).containsPattern(LIKE_BUTTON_WITHOUT_COUNT);
    }
}
