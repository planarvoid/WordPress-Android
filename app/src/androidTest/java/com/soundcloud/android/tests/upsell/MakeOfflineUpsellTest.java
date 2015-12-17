package com.soundcloud.android.tests.upsell;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.CollectionsTest;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.elements.PlaylistElement;

@EventTrackingTest
@CollectionsTest
public class MakeOfflineUpsellTest extends TrackingActivityTest<MainActivity> {

    private static final String LIKES_UPSELL_TEST_SCENARIO = "likes-upsell-tracking-test";
    private static final String PLAYLIST_ITEM_UPSELL_TEST_SCENARIO = "playlist-item-upsell-tracking-test";
    private static final String PLAYLIST_PAGE_UPSELL_TEST_SCENARIO = "playlist-page-upsell-tracking-test";

    public MakeOfflineUpsellTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.upsellUser.logIn(getInstrumentation().getTargetContext());
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.OFFLINE_SYNC);
        super.setUp();
    }

    public void testLikesUpsellImpressionAndClick() {
        TrackLikesScreen trackLikesScreen = mainNavHelper.goToTrackLikes();

        startEventTracking();

        UpgradeScreen upgradeScreen = trackLikesScreen
                .clickOverflowButton()
                .clickUpsell();

        assertThat(upgradeScreen, is(visible()));

        finishEventTracking(LIKES_UPSELL_TEST_SCENARIO);
    }

    public void testPlaylistItemUpsellImpressionAndClick() {
        PlaylistElement firstPlaylist = mainNavHelper.goToCollections().scrollToFirstPlaylist();

        startEventTracking();

        UpgradeScreen upgradeScreen = firstPlaylist
                .clickOverflow()
                .clickUpsell();

        assertThat(upgradeScreen, is(visible()));

        finishEventTracking(PLAYLIST_ITEM_UPSELL_TEST_SCENARIO);
    }

    public void testPlaylistPageImpressionAndClick() {
        CollectionsScreen collectionsScreen = mainNavHelper.goToCollections();

        startEventTracking();

        UpgradeScreen upgradeScreen = collectionsScreen
                .clickOnFirstPlaylist()
                .clickPlaylistOverflowButton()
                .clickUpsell();

        assertThat(upgradeScreen, is(visible()));

        finishEventTracking(PLAYLIST_PAGE_UPSELL_TEST_SCENARIO);
    }
}
