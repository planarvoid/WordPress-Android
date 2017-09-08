package com.soundcloud.android.tests.discovery.deeplinks;


import static android.net.Uri.parse;
import static com.soundcloud.android.R.string;
import static com.soundcloud.android.R.string.charts_electronic;
import static com.soundcloud.android.R.string.charts_page_header;
import static com.soundcloud.android.R.string.charts_trending;
import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.discovery.ChartsScreen;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;
import org.junit.Test;

import android.net.Uri;

public class GenreTrendingChartsWebDeeplink extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return parse("https://soundcloud.com/charts/new?genre=electronic");
    }

    @Test
    public void testTrendingChartsVisible() throws Exception {
        ChartsScreen screen = new ChartsScreen(solo);

        assertThat("Should display trending tab", screen.activeTabTitle().equals(getSolo().getString(charts_trending)));
        assertThat("Should show correct title", screen.getActionBarTitle().equals(getSolo().getString(charts_page_header, getSolo().getString(charts_electronic))));
    }
}
