package com.soundcloud.android.tests.search.intents;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.screens.discovery.SearchScreen;

import android.content.Intent;
import android.net.Uri;

public class ResolveSearchUrlTest extends SearchIntentsBaseTest {

    @Override
    protected Intent getIntent() {
        return new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/search/sounds"));
    }

    public void testSearchUrlResolution() {
        SearchScreen searchScreen = new SearchScreen(solo);
        assertThat("Search screen should be visible", searchScreen, is(visible()));
    }
}
