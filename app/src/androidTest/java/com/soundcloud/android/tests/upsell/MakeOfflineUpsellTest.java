package com.soundcloud.android.tests.upsell;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.elements.PlaylistElement;

public class MakeOfflineUpsellTest extends TrackingActivityTest<MainActivity> {

    private static final String LIKES_UPSELL_TEST_SCENARIO = "likes-upsell-tracking-test";
    private static final String PLAYLIST_ITEM_UPSELL_TEST_SCENARIO = "playlist-item-upsell-tracking-test";
    private static final String PLAYLIST_PAGE_UPSELL_TEST_SCENARIO = "playlist-page-upsell-tracking-test";

    public MakeOfflineUpsellTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.upsellUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    public void testLikesUpsellImpressionAndClick() {
        TrackLikesScreen trackLikesScreen = mainNavHelper.goToTrackLikes();

        startEventTracking();

        UpgradeScreen upgradeScreen = trackLikesScreen
                .toggleOfflineUpsell();

        assertThat(upgradeScreen, is(visible()));

        finishEventTracking(LIKES_UPSELL_TEST_SCENARIO);
    }

    public void testPlaylistPageImpressionAndClick() {
        CollectionScreen collectionScreen = mainNavHelper.goToCollections();

        startEventTracking();

        UpgradeScreen upgradeScreen = collectionScreen
                .clickPlaylistsPreview()
                .clickOnFirstPlaylist()
                .clickDownloadToggleForUpsell();

        assertThat(upgradeScreen, is(visible()));

        finishEventTracking(PLAYLIST_PAGE_UPSELL_TEST_SCENARIO);
    }

    public void testPlaylistItemUpsellImpressionAndClick() {
        PlaylistElement firstPlaylist = mainNavHelper.goToCollections()
                                                     .clickPlaylistsPreview()
                                                     .scrollToFirstPlaylist();

        startEventTracking();

        UpgradeScreen upgradeScreen = firstPlaylist
                .clickOverflow()
                .clickUpsell();

        assertThat(upgradeScreen, is(visible()));

        finishEventTracking(PLAYLIST_ITEM_UPSELL_TEST_SCENARIO);
    }
}
