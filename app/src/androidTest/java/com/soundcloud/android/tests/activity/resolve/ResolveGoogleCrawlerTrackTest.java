package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveGoogleCrawlerTrackTest extends ResolveGoogleCrawlerBaseTest {
    @Override
    protected Uri getUri() {
        return TestConsts.TWITTER_SOUND_URI;
    }

    public void testResolveUrl() {
        VisualPlayerElement playerScreen = new VisualPlayerElement(solo);
        assertThat(new StreamScreen(solo), is(visible()));
        playerScreen.waitForExpandedPlayer();
        assertThat(playerScreen, is(expanded()));
        assertThat(playerScreen.getTrackTitle(), is(equalToIgnoringCase("Tycho - From Home")));
    }

    public void testIsCrawlerUser() {
        VisualPlayerElement playerScreen = new VisualPlayerElement(solo);
        StreamScreen streamScreen = new StreamScreen(solo);

        playerScreen.waitForExpandedPlayer();
        playerScreen.pressCloseButton();

        MenuScreen menuScreen = streamScreen.openMenu();
        assertThat(menuScreen.getUserName(), is(equalTo("SoundCloud")));
    }
}
