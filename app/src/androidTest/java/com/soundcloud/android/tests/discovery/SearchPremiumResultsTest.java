package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.discovery.SearchPremiumResultsScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.discovery.SearchScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class SearchPremiumResultsTest extends ActivityTest<MainActivity> {

    private static final String PREMIUM_SEARCH_QUERY = "booht";

    private SearchScreen searchScreen;

    public SearchPremiumResultsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.searchHighTierUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.SEARCH_RESULTS_HIGH_TIER);
        super.setUp();
        searchScreen = mainNavHelper.goToDiscovery().clickSearch();
    }

    public void testSearchHighTierBucketIsVisible() {
        final SearchResultsScreen resultsScreen =
                searchScreen.setSearchQuery(PREMIUM_SEARCH_QUERY)
                        .clickOnCurrentSearchQuery();

        assertThat("Search premium content should be visible", resultsScreen.premiumContentIsVisible());
    }

    public void testClickOnPremiumTrackPlaysIt() {
        final VisualPlayerElement playerElement =
                searchScreen.setSearchQuery(PREMIUM_SEARCH_QUERY)
                        .clickOnCurrentSearchQuery()
                        .clickOnPremiumContent();

        assertThat("Player should be expanded", playerElement, is(expanded()));
    }

    public void testClickOnPremiumBucketHelpOpensUpgradeScreen() {
        final UpgradeScreen upgradeScreen =
                searchScreen.setSearchQuery(PREMIUM_SEARCH_QUERY)
                        .clickOnCurrentSearchQuery()
                        .clickOnPremiumContentHelp();

        assertThat("Upgrade subscription screen should be visible", upgradeScreen, is(visible()));
    }

    public void testClickOnPremiumContentBucketOpenSearchPremiumResults() {
        final SearchPremiumResultsScreen premiumResultsScreen =
                searchScreen.setSearchQuery(PREMIUM_SEARCH_QUERY)
                        .clickOnCurrentSearchQuery()
                        .clickOnViewPremiumContent();

        assertThat("Search premium results screen should be visible", premiumResultsScreen, is(visible()));
    }

    public void testPlaysPremiumTrackFromSearchPremiumResultsScreen() {
        final VisualPlayerElement playerElement =
                searchScreen.setSearchQuery(PREMIUM_SEARCH_QUERY)
                        .clickOnCurrentSearchQuery()
                        .clickOnViewPremiumContent()
                        .findAndClickFirstTrackItem();

        assertThat("Player should be expanded", playerElement, is(expanded()));
    }

    public void testClickOnUpsellOpensUpgradeScreen() {
        final UpgradeScreen upgradeScreen =
                searchScreen.setSearchQuery(PREMIUM_SEARCH_QUERY)
                        .clickOnCurrentSearchQuery()
                        .clickOnViewPremiumContent()
                        .clickOnUpgradeSubscription();

        assertThat("Upgrade subscription screen should be visible", upgradeScreen, is(visible()));
    }
}
