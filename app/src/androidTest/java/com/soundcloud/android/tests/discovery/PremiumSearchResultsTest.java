package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.SearchResultsHighTier;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.discovery.SearchPremiumResultsScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

@SearchResultsHighTier
public class PremiumSearchResultsTest extends ActivityTest<MainActivity> {

    private static final String PREMIUM_SEARCH_QUERY = "creator";

    private SearchResultsScreen searchResultsScreen;

    public PremiumSearchResultsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.upsellUser;
    }

    @Override
    protected void beforeStartActivity() {
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        searchResultsScreen = mainNavHelper
                .goToDiscovery()
                .clickSearch()
                .doSearch(PREMIUM_SEARCH_QUERY);
    }

    public void testSearchHighTierBucketIsOnScreen() {
        assertThat("Search premium content should be on screen", searchResultsScreen.premiumContentIsOnScreen());
    }

    public void testClickOnPremiumTrackPlaysIt() {
        final VisualPlayerElement playerElement = searchResultsScreen.clickOnPremiumContent();
        assertThat("Player should play premium track", playerElement, is(playing()));
    }

    public void testClickOnPremiumBucketHelpOpensUpgradeScreen() {
        final UpgradeScreen upgradeScreen = searchResultsScreen.clickOnPremiumContentHelp();
        assertThat("Upgrade subscription screen should be visible", upgradeScreen, is(visible()));
    }

    public void testClickOnPremiumContentBucketOpenSearchPremiumResults() {
        final SearchPremiumResultsScreen resultsScreen = searchResultsScreen.clickOnViewPremiumContent();
        assertThat("Search premium results screen should be visible", resultsScreen, is(visible()));
    }

    public void testPlaysPremiumTrackFromSearchPremiumResultsScreen() {
        final VisualPlayerElement playerElement =
                searchResultsScreen
                        .clickOnViewPremiumContent()
                        .findAndClickFirstTrackItem();

        assertThat("Player should play premium track", playerElement, is(playing()));
    }

    public void testClickOnUpsellOpensUpgradeScreen() {
        final UpgradeScreen upgradeScreen =
                searchResultsScreen
                        .clickOnViewPremiumContent()
                        .clickOnUpgradeSubscription();

        assertThat("Upgrade subscription screen should be visible", upgradeScreen, is(visible()));
    }
}
