package com.soundcloud.android.explore.genres;

import static com.soundcloud.android.tests.matcher.player.IsCollapsed.Collapsed;
import static com.soundcloud.android.tests.matcher.player.IsExpanded.Expanded;
import static com.soundcloud.android.tests.matcher.view.IsVisible.Visible;
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

    public void testVisualPlayerIsAccessible() throws Exception {
        player = streamScreen.clickFirstTrack();
        assertThat(player, is(Expanded()));
        player.pressBackToCollapse();

        openExploreGenreAmbient();
        assertThat(player, is(Visible()));
        assertThat(player, is(Collapsed()));
    }

    public void testPlayerIsNotVisibleIfNothingIsPlaying() throws Exception {
        openExploreGenreAmbient();

        assertThat(new VisualPlayerElement(solo), is(not(Visible())));
    }

    private void openExploreGenreAmbient() {
        final ExploreScreen exploreScreen = NavigationHelper.openExploreFromMenu(streamScreen);
        exploreScreen.touchGenresTab();
        categoryScreen = exploreScreen.clickGenreItem("Ambient");
    }
}
