package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.search.SearchResultsFragment;
import com.soundcloud.android.search.topresults.TopResultsBucketActivity;

import java.util.regex.Pattern;

public class SearchTrackResultsScreen extends Screen {

    private static final Class ACTIVITY = TopResultsBucketActivity.class;
    private static final String FRAGMENT = SearchResultsFragment.TAG;

    SearchTrackResultsScreen(Han solo) {
        super(solo);
        waiter.assertForFragmentByTag(FRAGMENT);
    }

    public SearchTopResultsScreen pressBack() {
        testDriver.goBack();
        return new SearchTopResultsScreen(testDriver);
    }

    public UpgradeScreen clickOnUpgradeSubscription() {
        testDriver.findOnScreenElement(With.text(R.string.search_premium_content_upsell)).click();
        return new UpgradeScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForFragmentByTag(FRAGMENT);
    }

    public ViewElement goTracksCountHeader() {
        return scrollToItem(With.textMatching(Pattern.compile("Found .+ Go[+] tracks", Pattern.CASE_INSENSITIVE)));
    }
}
