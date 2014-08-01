package com.soundcloud.android.search;

import static com.soundcloud.android.tests.matcher.player.IsCollapsed.Collapsed;
import static com.soundcloud.android.tests.matcher.player.IsExpanded.Expanded;
import static com.soundcloud.android.tests.matcher.view.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.search.SearchResultsScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;
import com.soundcloud.android.tests.helpers.NavigationHelper;

import java.lang.reflect.InvocationTargetException;

public class Player extends ActivityTestCase<MainActivity> {
    private StreamScreen streamScreen;
    private SearchResultsScreen searchResultsScreen;
    private VisualPlayerElement player;

    public Player() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
    }

    public void testVisualPlayerIsAccessibleFromSearch() throws Exception {
        streamScreen().clickFirstTrack();
        assertThat(player(), is(Expanded()));
        player().pressBackToCollapse();

        NavigationHelper.openSearch(streamScreen());
        assertThat(player(), is(Visible()));
        assertThat(player(), is(Collapsed()));
    }

    public void testPlayerIsNotVisibleIfNothingIsPlaying() throws Exception {
        NavigationHelper.openSearch(streamScreen());

        assertThat(player(), is(not(Visible())));
    }

    public void testTapingATrackFromSearchOpenVisualPlayer() throws Exception {
        NavigationHelper.openSearch(streamScreen());
        searchScreen().actionBar().doSearch("nasa");
        player = searchScreen().clickFirstTrackItem();

        assertThat(player, is(Expanded()));
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
