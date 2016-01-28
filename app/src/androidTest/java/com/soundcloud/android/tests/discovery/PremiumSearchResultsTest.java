package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.SearchResultsHighTier;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.discovery.SearchPremiumResultsScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.discovery.SearchScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

@SearchResultsHighTier
public class PremiumSearchResultsTest extends ActivityTest<MainActivity> {

    private static final String PREMIUM_SEARCH_QUERY = "booht";

    private SearchScreen searchScreen;

    public PremiumSearchResultsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.searchHighTierUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        searchScreen = mainNavHelper.goToDiscovery().clickSearch();
    }

    public void testSearchHighTierBucketIsOnScreen() {
        final SearchResultsScreen searchResultsScreen =
                searchScreen
                        .setSearchQuery(PREMIUM_SEARCH_QUERY)
                        .clickOnCurrentSearchQuery();
        assertThat("Search premium content should be on screen", searchResultsScreen.premiumContentIsOnScreen());
    }

    public void testClickOnPremiumTrackPlaysIt() {
        final VisualPlayerElement playerElement =
                searchScreen
                        .setSearchQuery(PREMIUM_SEARCH_QUERY)
                        .clickOnCurrentSearchQuery()
                        .clickOnPremiumContent();
        assertThat("Player should play premium track", playerElement, is(playing()));
    }

    public void testClickOnPremiumBucketHelpOpensUpgradeScreen() {
        final UpgradeScreen upgradeScreen =
                searchScreen
                        .setSearchQuery(PREMIUM_SEARCH_QUERY)
                        .clickOnCurrentSearchQuery()
                        .clickOnPremiumContentHelp();
        assertThat("Upgrade subscription screen should be visible", upgradeScreen, is(visible()));
    }

    public void testClickOnPremiumContentBucketOpenSearchPremiumResults() {
        final SearchPremiumResultsScreen resultsScreen =
                searchScreen
                        .setSearchQuery(PREMIUM_SEARCH_QUERY)
                        .clickOnCurrentSearchQuery()
                        .clickOnViewPremiumContent();
        assertThat("Search premium results screen should be visible", resultsScreen, is(visible()));
    }

    public void testPlaysPremiumTrackFromSearchPremiumResultsScreen() {
        final VisualPlayerElement playerElement =
                searchScreen
                        .setSearchQuery(PREMIUM_SEARCH_QUERY)
                        .clickOnCurrentSearchQuery()
                        .clickOnViewPremiumContent()
                        .findAndClickFirstTrackItem();
        assertThat("Player should play premium track", playerElement, is(playing()));
    }

    public void testClickOnUpsellOpensUpgradeScreen() {
        final UpgradeScreen upgradeScreen =
                searchScreen
                        .setSearchQuery(PREMIUM_SEARCH_QUERY)
                        .clickOnCurrentSearchQuery()
                        .clickOnViewPremiumContent()
                        .clickOnUpgradeSubscription();
        assertThat("Upgrade subscription screen should be visible", upgradeScreen, is(visible()));
    }
}
