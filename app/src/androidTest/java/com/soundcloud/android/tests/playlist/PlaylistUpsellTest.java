package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.tests.TestConsts;

import android.content.Intent;

public class PlaylistUpsellTest extends TrackingActivityTest<MainActivity> {

    public PlaylistUpsellTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(TestConsts.PLAYLIST_UPSELL));
        super.setUp();
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.upsellUser;
    }

    @Override
    protected void beforeStartActivity() {
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    public void testUserCanNavigateToSubscribePageFromUpsell() {
        PlaylistDetailsScreen playlistDetailsScreen = new PlaylistDetailsScreen(solo);

        assertThat(playlistDetailsScreen, is(visible()));
        assertThat(playlistDetailsScreen.getTitle(), is(equalToIgnoringCase("HT 1 - 4")));

        UpgradeScreen upgradeScreen = playlistDetailsScreen.scrollToUpsell().clickUpgrade();
        assertThat(upgradeScreen, is(visible()));

        solo.goBack();

        // todo, Mr. LL tracking validation
    }
}
