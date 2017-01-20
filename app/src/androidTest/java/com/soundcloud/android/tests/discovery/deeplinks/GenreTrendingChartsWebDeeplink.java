package com.soundcloud.android.tests.discovery.deeplinks;


import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.discovery.ChartsScreen;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;

import android.net.Uri;

public class GenreTrendingChartsWebDeeplink extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return Uri.parse("https://soundcloud.com/charts/new?genre=electronic");
    }

    public void testTrendingChartsVisible() throws Exception {
        ChartsScreen screen = new ChartsScreen(solo);

        assertThat("Should display trending tab", screen.activeTabTitle().equals(getSolo().getString(R.string.charts_trending)));
        assertThat("Should show correct title", screen.getActionBarTitle().equals(getSolo().getString(R.string.charts_page_header, getSolo().getString(R.string.charts_electronic))));
    }
}
