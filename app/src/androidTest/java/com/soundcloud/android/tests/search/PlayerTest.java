package com.soundcloud.android.tests.search;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class PlayerTest extends ActivityTest<MainActivity> {
    private VisualPlayerElement player;

    public PlayerTest() {
        super(MainActivity.class);
    }

    @Override
    protected void beforeStartActivity() {
        FeatureFlagsHelper.create(getInstrumentation().getTargetContext()).disable(Flag.SEARCH_TOP_RESULTS);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.playlistUser;
    }

    public void testVisualPlayerIsAccessibleFromSearch() {
        final VisualPlayerElement player = mainNavHelper
                .goToStream()
                .clickFirstTrackCard();

        assertThat(player, is(expanded()));
        player.pressBackToCollapse();

        mainNavHelper.goToOldDiscovery();

        assertThat(player(), is(visible()));
        assertThat(player(), is(collapsed()));
    }

    public void testPlayerIsNotVisibleIfNothingIsPlaying() {
        mainNavHelper.goToOldDiscovery();

        assertThat(player(), is(not(visible())));
    }

    public void testTapingATrackFromSearchOpenVisualPlayer() {
        final VisualPlayerElement player = mainNavHelper
                .goToOldDiscovery()
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
