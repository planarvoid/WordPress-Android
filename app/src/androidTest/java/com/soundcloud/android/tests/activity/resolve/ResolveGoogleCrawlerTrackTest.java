package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.YouScreen;
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
        assertThat(playerScreen.getTrackTitle(), is(equalToIgnoringCase("From Home")));
    }

    public void testIsCrawlerUser() {
        VisualPlayerElement playerScreen = new VisualPlayerElement(solo);

        playerScreen.waitForExpandedPlayer();
        playerScreen.pressCloseButton();

        YouScreen youScreen = mainNavHelper.goToYou();
        assertThat(youScreen.getUserName(), is(equalTo("")));
    }
}
