package com.soundcloud.android.tests.search;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.search.SearchResultsScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.TestUser;

import java.lang.reflect.InvocationTargetException;

public class PlayerTest extends ActivityTest<MainActivity> {
    private StreamScreen streamScreen;
    private SearchResultsScreen searchResultsScreen;
    private VisualPlayerElement player;

    public PlayerTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testVisualPlayerIsAccessibleFromSearch() throws Exception {
        streamScreen().clickFirstTrack();
        player().waitForContent();
        assertThat(player(), is(expanded()));
        player().pressBackToCollapse();

        streamScreen().actionBar().clickSearchButton();
        assertThat(player(), is(visible()));
        assertThat(player(), is(collapsed()));
    }

    public void testPlayerIsNotVisibleIfNothingIsPlaying() throws Exception {
        streamScreen().actionBar().clickSearchButton();

        assertThat(player(), is(not(visible())));
    }

    public void testTapingATrackFromSearchOpenVisualPlayer() throws Exception {
        streamScreen().actionBar().clickSearchButton();
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
