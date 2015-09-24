package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.net.Uri;
import android.widget.AutoCompleteTextView;

public class SearchControllerTest extends AndroidUnitTest {

    private SearchController searchController;

    @Mock private SuggestionsAdapter adapter;

    private AutoCompleteTextView searchView;
    private MySearchCallback callback;

    @Before
    public void setUp() {
        searchController = new SearchController(adapter);
        searchView = new AutoCompleteTextView(context());
        callback = new MySearchCallback();
    }

    @Test
    public void performsTextSearch() {
        when(adapter.isSearchItem(anyInt())).thenReturn(true);

        searchController.bindSearchView(searchView, callback);
        clickSearchItem();

        assertThat(callback.IsPerformTextSearchCalled()).isTrue();
        assertThat(callback.isLaunchSearchSuggestion()).isFalse();
    }

    @Test
    public void launchesSearchSuggestion() {
        when(adapter.isSearchItem(anyInt())).thenReturn(false);
        when(adapter.getItemIntentData(anyInt())).thenReturn(Uri.EMPTY);
        when(adapter.getQueryUrn(anyInt())).thenReturn(Urn.NOT_SET);

        searchController.bindSearchView(searchView, callback);
        clickSearchItem();

        assertThat(callback.IsPerformTextSearchCalled()).isFalse();
        assertThat(callback.isLaunchSearchSuggestion()).isTrue();
    }

    private void clickSearchItem() {
        searchView.getOnItemClickListener().onItemClick(null, null, 0, 0);
    }

    private static class MySearchCallback implements SearchController.SearchCallback {
        private boolean performTextSearchCalled = false;
        private boolean launchSearchSuggestion = false;

        @Override
        public void performTextSearch(Context context, String query) {
            performTextSearchCalled = true;
        }

        @Override
        public void launchSearchSuggestion(Context context, Urn urn, SearchQuerySourceInfo searchQuerySourceInfo, Uri itemUri) {
            launchSearchSuggestion = true;
        }

        boolean IsPerformTextSearchCalled() {
            return performTextSearchCalled;
        }

        boolean isLaunchSearchSuggestion() {
            return launchSearchSuggestion;
        }
    }
}