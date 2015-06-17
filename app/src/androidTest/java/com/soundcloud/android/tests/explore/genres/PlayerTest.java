package com.soundcloud.android.tests.explore.genres;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTest;

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
        streamScreen = new StreamScreen(solo);
    }

    public void testVisualPlayerIsAccessible() {
        final VisualPlayerElement player = streamScreen.clickFirstTrack();
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
        final ExploreScreen exploreScreen = streamScreen.openExploreFromMenu();
        exploreScreen.touchGenresTab();
        exploreScreen.clickGenreItem("Ambient");
    }
}
