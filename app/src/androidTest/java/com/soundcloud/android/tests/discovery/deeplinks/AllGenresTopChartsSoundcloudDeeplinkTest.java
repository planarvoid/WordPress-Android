package com.soundcloud.android.tests.discovery.deeplinks;


import static android.net.Uri.parse;
import static com.soundcloud.android.R.string;
import static com.soundcloud.android.R.string.charts_header;
import static com.soundcloud.android.R.string.charts_top;
import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.discovery.ChartsScreen;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;
import org.junit.Test;

import android.net.Uri;

public class AllGenresTopChartsSoundcloudDeeplinkTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return parse("soundcloud://charts:top:all");
    }

    @Test
    public void testTopChartsVisible() throws Exception {
        ChartsScreen screen = new ChartsScreen(solo);

        assertThat("Tab should be Top 50", screen.activeTabTitle().equals(solo.getString(charts_top)));
        assertThat("Title should be default header", screen.getActionBarTitle().equals(solo.getString(charts_header)));
    }
}
