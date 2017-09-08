package com.soundcloud.android.tests.playlist;

import static android.content.Intent.ACTION_VIEW;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.soundcloud.android.framework.TestUser.upsellUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableUpsell;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.TestConsts.PLAYLIST_UPSELL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.content.Intent;

public class PlaylistUpsellTest extends ActivityTest<MainActivity> {

    public PlaylistUpsellTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        setActivityIntent(new Intent(ACTION_VIEW).setData(PLAYLIST_UPSELL));
        super.setUp();
    }

    @Override
    protected TestUser getUserForLogin() {
        return upsellUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        enableUpsell(getInstrumentation().getTargetContext());
    }

    @Test
    public void testUserCanNavigateToSubscribePageFromUpsell() throws Exception {
        PlaylistDetailsScreen playlistDetailsScreen = new PlaylistDetailsScreen(solo);

        assertThat(playlistDetailsScreen, is(visible()));
        assertThat(playlistDetailsScreen.getTitle(), is(equalToIgnoringCase("HT 1 - 4")));

        UpgradeScreen upgradeScreen = playlistDetailsScreen.scrollToUpsell().clickUpgrade();
        assertThat(upgradeScreen, is(visible()));

        solo.goBack();

        // todo, Mr. LL tracking validation
    }
}
