package com.soundcloud.android.tests.discovery.deeplinks;


import static android.net.Uri.parse;
import static com.soundcloud.android.R.string;
import static com.soundcloud.android.R.string.charts_music;
import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.discovery.AllGenresScreen;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;
import org.junit.Test;

import android.net.Uri;

public class GenreOverviewDeeplink extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return parse("soundcloud://charts:music");
    }

    @Test
    public void testTopChartsVisible() throws Exception {
        AllGenresScreen screen = new AllGenresScreen(solo);

        assertThat("Should display music tab", screen.activeTabTitle().equalsIgnoreCase(getSolo().getString(charts_music)));
    }
}
