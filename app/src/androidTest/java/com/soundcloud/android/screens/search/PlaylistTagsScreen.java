package com.soundcloud.android.screens.search;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.search.LegacySearchActivity;

/**
 * @deprecated since this is part of Legacy Search Feature.
 * Will be remove it once this is done:
 * https://soundcloud.atlassian.net/browse/SEARCH-433
 */
@Deprecated
public class PlaylistTagsScreen extends Screen {

    private static final Class ACTIVITY = LegacySearchActivity.class;

    public PlaylistTagsScreen(Han solo) {
        super(solo);
        waiter.waitForElement(R.id.all_tags);
    }

    @Override
    public boolean isVisible() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return waiter.waitForElement(R.id.all_tags);
    }

    public StreamScreen pressBack() {
        testDriver.goBack();
        return new StreamScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
