package com.soundcloud.android.tests.search.intents;

import static android.content.Intent.ACTION_VIEW;
import static android.net.Uri.parse;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.discovery.SearchResultsScreen;
import org.junit.Test;

import android.content.Intent;
import android.net.Uri;

public class ResolveSearchUrlWithQueryTest extends SearchIntentsBaseTest {

    @Override
    protected Intent getIntent() {
        return new Intent(ACTION_VIEW).setData(parse("https://soundcloud.com/search/sounds?q=skrillex"));
    }

    @Test
    public void testSearchQueryUrlResolution() throws Exception {
        SearchResultsScreen resultsScreen = new SearchResultsScreen(solo);
        assertThat("Search results screen should be visible", resultsScreen, is(visible()));
        assertThat("Search query should be set", resultsScreen.actionBar().getSearchQuery(), is("skrillex"));
    }
}
