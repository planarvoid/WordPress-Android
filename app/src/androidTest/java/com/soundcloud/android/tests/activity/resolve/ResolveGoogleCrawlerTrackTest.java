package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.MoreScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveGoogleCrawlerTrackTest extends ResolveGoogleCrawlerBaseTest {
    @Override
    protected Uri getUri() {
        return TestConsts.CHE_FLUTE_DEEP_LINK;
    }

    public void testResolveUrl() {
        VisualPlayerElement playerScreen = new VisualPlayerElement(solo);
        assertThat(new StreamScreen(solo), is(visible()));
        playerScreen.waitForExpandedPlayer();
        assertThat(playerScreen, is(expanded()));
        assertThat(playerScreen.getTrackTitle(), is(equalTo("STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]")));
    }

    public void testIsCrawlerUser() {
        VisualPlayerElement playerScreen = new VisualPlayerElement(solo);

        playerScreen.waitForExpandedPlayer();
        playerScreen.pressCloseButton();

        MoreScreen moreScreen = mainNavHelper.goToMore();
        assertThat(moreScreen.getUserName(), is(equalTo("")));
    }
}
