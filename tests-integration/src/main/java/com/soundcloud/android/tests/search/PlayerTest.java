package com.soundcloud.android.tests.search;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.framework.screens.StreamScreen;
import com.soundcloud.android.framework.screens.elements.VisualPlayerElement;
import com.soundcloud.android.framework.screens.search.SearchResultsScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.NavigationHelper;

import java.lang.reflect.InvocationTargetException;

public class PlayerTest extends ActivityTest<MainActivity> {
    private StreamScreen streamScreen;
    private SearchResultsScreen searchResultsScreen;
    private VisualPlayerElement player;

    public PlayerTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
    }

    public void testVisualPlayerIsAccessibleFromSearch() throws Exception {
        streamScreen().clickFirstTrack();
        player().waitForContent();
        assertThat(player(), is(expanded()));
        player().pressBackToCollapse();

        NavigationHelper.openSearch(streamScreen());
        assertThat(player(), is(visible()));
        assertThat(player(), is(collapsed()));
    }

    public void testPlayerIsNotVisibleIfNothingIsPlaying() throws Exception {
        NavigationHelper.openSearch(streamScreen());

        assertThat(player(), is(not(visible())));
    }

    public void testTapingATrackFromSearchOpenVisualPlayer() throws Exception {
        NavigationHelper.openSearch(streamScreen());
        searchScreen().actionBar().doSearch("nasa");
        player = searchScreen().clickFirstTrackItem();

        assertThat(player, is(expanded()));
    }

    private VisualPlayerElement player() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (player == null) {
            player = new VisualPlayerElement(solo);
        }
        return player;
    }

    private SearchResultsScreen searchScreen() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (searchResultsScreen == null) {
            searchResultsScreen = new SearchResultsScreen(solo);
        }
        return searchResultsScreen;
    }


    private StreamScreen streamScreen() {
        if (streamScreen == null) {
            streamScreen = new StreamScreen(solo);
        }
        return streamScreen;
    }
}
