package com.soundcloud.android.tests.search.intents;

import static android.content.Intent.ACTION_VIEW;
import static android.net.Uri.parse;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.screens.discovery.SearchScreen;
import org.junit.Test;

import android.content.Intent;
import android.net.Uri;

public class ResolveSearchUrlTest extends SearchIntentsBaseTest {

    @Override
    protected Intent getIntent() {
        return new Intent(ACTION_VIEW).setData(parse("http://soundcloud.com/search/sounds"));
    }

    @Test
    public void testSearchUrlResolution() throws Exception {
        SearchScreen searchScreen = new SearchScreen(solo);
        assertThat("Search screen should be visible", searchScreen, is(visible()));
    }
}
