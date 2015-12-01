package com.soundcloud.android.tests.search;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class SearchNavigationTest extends ActivityTest<MainActivity> {

    private DiscoveryScreen discoveryScreen;

    public SearchNavigationTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        discoveryScreen = mainNavHelper.goToDiscovery();
    }

    public void testShouldOpenDiscoveryTappingOnSearchIcon() {
        assertThat(discoveryScreen, is(visible()));
    }

    public void testGoingBackFromSearchResultsReturnsToDiscoveryScreen() {
        final SearchResultsScreen resultsScreen = discoveryScreen.clickSearch().doSearch("clownstep");
        final DiscoveryScreen discoveryScreen = resultsScreen.goBack();

        assertThat("Tags screen should be visible", discoveryScreen, is(visible()));
    }
}
