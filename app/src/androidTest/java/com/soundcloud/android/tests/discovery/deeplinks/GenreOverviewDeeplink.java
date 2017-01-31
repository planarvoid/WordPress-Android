package com.soundcloud.android.tests.discovery.deeplinks;


import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.discovery.AllGenresScreen;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;

import android.net.Uri;

public class GenreOverviewDeeplink extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return Uri.parse("soundcloud://charts:music");
    }

    public void testTopChartsVisible() throws Exception {
        AllGenresScreen screen = new AllGenresScreen(solo);

        assertThat("Should display music tab", screen.activeTabTitle().equalsIgnoreCase(getSolo().getString(R.string.charts_music)));
    }
}
