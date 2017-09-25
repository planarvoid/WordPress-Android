package com.soundcloud.android.tests.search;

import static com.soundcloud.android.framework.TestUser.playlistUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class PlayerTest extends ActivityTest<MainActivity> {
    private VisualPlayerElement player;

    public PlayerTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return playlistUser;
    }

    @Test
    public void testVisualPlayerIsAccessibleFromSearch() throws Exception {
        final VisualPlayerElement player = mainNavHelper
                .goToStream()
                .clickFirstTrackCard();

        assertThat(player, is(expanded()));
        player.pressBackToCollapse();

        mainNavHelper.goToDiscovery();

        assertThat(player(), is(visible()));
        assertThat(player(), is(collapsed()));
    }

    @Test
    public void testPlayerIsNotVisibleIfNothingIsPlaying() throws Exception {
        mainNavHelper.goToDiscovery();

        assertThat(player(), is(not(visible())));
    }

    @Test
    public void testTapingATrackFromSearchOpenVisualPlayer() throws Exception {
        final VisualPlayerElement player = mainNavHelper
                .goToDiscovery()
                .clickSearch()
                .doSearch("nasa")
                .goToTracksTab()
                .findAndClickFirstTrackItem();

        assertThat(player, is(expanded()));
    }

    private VisualPlayerElement player() {
        if (player == null) {
            player = new VisualPlayerElement(solo);
        }
        return player;
    }
}
