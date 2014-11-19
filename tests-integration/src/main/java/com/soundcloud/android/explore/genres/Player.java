package com.soundcloud.android.explore.genres;

import static com.soundcloud.android.tests.matcher.element.IsVisible.visible;
import static com.soundcloud.android.tests.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.tests.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreGenreCategoryScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;
import com.soundcloud.android.tests.helpers.NavigationHelper;

public class Player extends ActivityTestCase<MainActivity> {
    private VisualPlayerElement player;
    private StreamScreen streamScreen;
    private ExploreGenreCategoryScreen categoryScreen;

    public Player() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.streamUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
        streamScreen = new StreamScreen(solo);
    }

    public void testVisualPlayerIsAccessible() {
        player = streamScreen.clickFirstTrack();
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
        final ExploreScreen exploreScreen = NavigationHelper.openExploreFromMenu(streamScreen);
        exploreScreen.touchGenresTab();
        categoryScreen = exploreScreen.clickGenreItem("Ambient");
    }
}
