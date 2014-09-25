package com.soundcloud.android.search;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.view.ListViewController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;

import android.os.Bundle;

@RunWith(SoundCloudTestRunner.class)
public class SearchResultsFragmentTest {


    private SearchResultsFragment fragment;

    @Mock private SearchOperations operations;
    @Mock private SearchOperations.SearchResultPager pager;
    @Mock private Subscription subscription;
    @Mock private ListViewController listViewController;

    @Before
    public void setUp() {
        fragment = createFragment("query", SearchOperations.TYPE_ALL);

        final Observable<ModelCollection<PropertySetSource>> searchResult =
                TestObservables.withSubscription(subscription, Observable.<ModelCollection<PropertySetSource>>never());
        when(operations.getSearchResult("query", SearchOperations.TYPE_ALL)).thenReturn(searchResult);
        when(operations.pager(SearchOperations.TYPE_ALL)).thenReturn(pager);
        when(pager.page(searchResult)).thenReturn(searchResult);
    }

    @Test
    public void shouldUnsubscribeFromSourceObservableInOnDestroy() {
        fragment.onCreate(null);
        fragment.onDestroy();
        verify(subscription).unsubscribe();
    }

    private SearchResultsFragment createFragment(String query, int searchType) {
        SearchResultsFragment fragment = new SearchResultsFragment(operations, listViewController);

        Bundle bundle = new Bundle();
        bundle.putInt(SearchResultsFragment.EXTRA_TYPE, searchType);
        bundle.putString(SearchResultsFragment.EXTRA_QUERY, query);
        fragment.setArguments(bundle);
        return fragment;
    }

}