package com.soundcloud.android.tests.upsell;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.configuration.experiments.PlaylistAndAlbumsPreviewsExperiment;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.tests.ActivityTest;

public class MakeOfflineUpsellTest extends ActivityTest<MainActivity> {

    private static final String LIKES_UPSELL_TEST_SCENARIO = "specs/likes-upsell-tracking-test.spec";
    private static final String PLAYLIST_ITEM_UPSELL_TEST_SCENARIO = "specs/playlist-item-upsell-tracking-test.spec";
    private static final String PLAYLIST_PAGE_UPSELL_TEST_SCENARIO = "specs/playlist-page-upsell-tracking-test.spec";

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

        getExperiments().set(PlaylistAndAlbumsPreviewsExperiment.CONFIGURATION, PlaylistAndAlbumsPreviewsExperiment.VARIANT_CONTROL);

        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    public void testLikesUpsellImpressionAndClick() throws Exception {
        TrackLikesScreen trackLikesScreen = mainNavHelper.goToTrackLikes();

        mrLocalLocal.startEventTracking();

        UpgradeScreen upgradeScreen = trackLikesScreen
                .toggleOfflineUpsell();

        assertThat(upgradeScreen, is(visible()));

        mrLocalLocal.verify(LIKES_UPSELL_TEST_SCENARIO);
    }

    public void testPlaylistPageImpressionAndClick() throws Exception {
        CollectionScreen collectionScreen = mainNavHelper.goToCollections();

        mrLocalLocal.startEventTracking();

        UpgradeScreen upgradeScreen = collectionScreen
                .clickPlaylistsPreview()
                .clickOnFirstPlaylist()
                .clickDownloadButtonForUpsell();

        assertThat(upgradeScreen, is(visible()));

        mrLocalLocal.verify(PLAYLIST_PAGE_UPSELL_TEST_SCENARIO);
    }

    public void testPlaylistItemUpsellImpressionAndClick() throws Exception {
        PlaylistElement firstPlaylist = mainNavHelper.goToCollections()
                                                     .clickPlaylistsPreview()
                                                     .scrollToFirstPlaylist();

        mrLocalLocal.startEventTracking();

        UpgradeScreen upgradeScreen = firstPlaylist
                .clickOverflow()
                .clickUpsell();

        assertThat(upgradeScreen, is(visible()));

        mrLocalLocal.verify(PLAYLIST_ITEM_UPSELL_TEST_SCENARIO);
    }
}
