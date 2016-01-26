package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.search.SearchPremiumResultsActivity;
import com.soundcloud.android.search.SearchPremiumResultsFragment;

public class SearchPremiumResultsScreen extends Screen {

    private static final Class ACTIVITY = SearchPremiumResultsActivity.class;
    private static final String FRAGMENT = SearchPremiumResultsFragment.TAG;

    public SearchPremiumResultsScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag(FRAGMENT);
    }

    public UpgradeScreen clickOnUpgradeSubscription() {
        upgradeSubscriptionView().click();
        return new UpgradeScreen(testDriver);
    }

    public VisualPlayerElement findAndClickFirstTrackItem() {
        scrollToItem(With.id(R.id.track_list_item)).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public SearchResultsScreen goBack() {
        testDriver.goBack();
        return new SearchResultsScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForFragmentByTag(FRAGMENT);
    }

    private ViewElement upgradeSubscriptionView() {
        return testDriver.findOnScreenElement(With.id(R.id.search_upsell));
    }
}
