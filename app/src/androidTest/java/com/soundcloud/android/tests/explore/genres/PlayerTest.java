package com.soundcloud.android.tests.explore.genres;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

public class PlayerTest extends ActivityTest<MainActivity> {
    private StreamScreen streamScreen;

    public PlayerTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.streamUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Context context = getInstrumentation().getTargetContext();
        ConfigurationHelper.disableIntroductoryOverlay(context, IntroductoryOverlayKey.PLAY_QUEUE);

        streamScreen = new StreamScreen(solo);
    }

    public void testVisualPlayerIsAccessible() {
        final VisualPlayerElement player = streamScreen.clickFirstTrackCard();
        assertThat(player, is(expanded()));
        player.pressBackToCollapse();

        openExploreGenreAmbient();
        assertThat(player, is(visible()));
        assertThat(player, is(collapsed()));
    }

    public void testPlayerIsNotVisibleIfNothingIsPlaying() {
        openExploreGenreAmbient();

        assertThat(new VisualPlayerElement(solo), is(not(visible())));
    }

    private void openExploreGenreAmbient() {
        final ExploreScreen exploreScreen = mainNavHelper.goToExplore();
        exploreScreen.touchGenresTab();
        exploreScreen.clickGenreItem("Ambient");
    }
}
