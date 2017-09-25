package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.TestUser.upsellUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableUpsell;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.discovery.SearchPremiumResultsScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class PremiumSearchResultsTest extends ActivityTest<MainActivity> {

    private static final String PREMIUM_SEARCH_QUERY = "creator";

    private SearchResultsScreen searchResultsScreen;

    public PremiumSearchResultsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return upsellUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        enableUpsell(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        searchResultsScreen = mainNavHelper
                .goToDiscovery()
                .clickSearch()
                .doSearch(PREMIUM_SEARCH_QUERY);
    }

    @Test
    public void testSearchHighTierBucketIsOnScreen() throws Exception {
        assertThat("Search premium content should be on screen", searchResultsScreen.premiumContentIsOnScreen());
    }

    @Test
    public void testClickOnPremiumTrackPlaysIt() throws Exception {
        final VisualPlayerElement playerElement = searchResultsScreen.clickOnPremiumContent();
        assertThat("Player should play premium track", playerElement, is(playing()));
    }

    @org.junit.Ignore
    @Ignore
    /** Test cannot find Premium Content, even though it is displayed on screen.
     *  JIRA Ticket: https://soundcloud.atlassian.net/browse/DROID-1304 */
    @Test
    public void testClickOnPremiumBucketHelpOpensUpgradeScreen() {
        final UpgradeScreen upgradeScreen = searchResultsScreen.clickOnPremiumContentHelp();
        assertThat("Upgrade subscription screen should be visible", upgradeScreen, is(visible()));
    }

    @Test
    public void testClickOnPremiumContentBucketOpenSearchPremiumResults() throws Exception {
        final SearchPremiumResultsScreen resultsScreen = searchResultsScreen.clickOnViewPremiumContent();
        assertThat("Search premium results screen should be visible", resultsScreen, is(visible()));
    }

    @Test
    public void testPlaysPremiumTrackFromSearchPremiumResultsScreen() throws Exception {
        final VisualPlayerElement playerElement =
                searchResultsScreen
                        .clickOnViewPremiumContent()
                        .findAndClickFirstTrackItem();

        assertThat("Player should play premium track", playerElement, is(playing()));
    }

    @Test
    public void testClickOnUpsellOpensUpgradeScreen() throws Exception {
        final UpgradeScreen upgradeScreen =
                searchResultsScreen
                        .clickOnViewPremiumContent()
                        .clickOnUpgradeSubscription();

        assertThat("Upgrade subscription screen should be visible", upgradeScreen, is(visible()));
    }
}
