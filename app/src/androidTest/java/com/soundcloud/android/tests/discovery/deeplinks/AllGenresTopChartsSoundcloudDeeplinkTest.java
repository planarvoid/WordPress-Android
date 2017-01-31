package com.soundcloud.android.tests.discovery.deeplinks;


import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.discovery.ChartsScreen;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;

import android.net.Uri;

public class AllGenresTopChartsSoundcloudDeeplinkTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return Uri.parse("soundcloud://charts:top:all");
    }

    public void testTopChartsVisible() throws Exception {
        ChartsScreen screen = new ChartsScreen(solo);

        assertThat("Tab should be Top 50", screen.activeTabTitle().equals(solo.getString(R.string.charts_top)));
        assertThat("Title should be default header", screen.getActionBarTitle().equals(solo.getString(R.string.charts_header)));
    }
}
