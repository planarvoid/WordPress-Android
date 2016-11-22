package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SearchSuggestionsPresenter.SuggestionListener;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscriber;
import rx.observers.TestSubscriber;

import android.os.Bundle;
import android.view.View;

import java.util.List;

public class SearchSuggestionsPresenterTest extends AndroidUnitTest {

    private static final String SEARCH_QUERY = "query";
    private static final int CLICK_POSITION = 1;

    private SearchSuggestionsPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private SuggestionsAdapter adapter;
    @Mock private SearchSuggestionOperations operations;
    @Mock private SuggestionListener suggestionListener;
    @Mock private List<SuggestionItem> suggestionItems;
    @Mock private View view;

    @Rule public FragmentRule fragmentRule = new FragmentRule(R.layout.recyclerview_with_emptyview);

    private TestSubscriber<List<SuggestionItem>> testSubscriber = new TestSubscriber<>();

    @Before
    public void setUp() {
        presenter = new SearchSuggestionsPresenter(swipeRefreshAttacher, adapter, operations);
        when(operations.suggestionsFor(anyString())).thenReturn(Observable.just(suggestionItems));

        presenter.setSuggestionListener(suggestionListener);
    }

    @Test
    public void triggersSearchEventOnSearchItemClicked() {
        final SuggestionItem suggestionItem = SuggestionItem.forSearch(SEARCH_QUERY);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(mock(View.class), CLICK_POSITION);

        verify(suggestionListener).onSearchClicked(SEARCH_QUERY);
    }

    @Test
    public void triggersTrackClickEventOnTrackItemClicked() {
        final SuggestionItem suggestionItem = SuggestionItem.forTrack(PropertySet.create().put(SearchSuggestionProperty.URN, Urn.forTrack(123)), SEARCH_QUERY);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(view, CLICK_POSITION);

        verify(suggestionListener).onSuggestionClicked(suggestionItem);
    }

    @Test
    public void triggersUserClickEventOnUserItemClicked() {
        final SuggestionItem suggestionItem = SuggestionItem.forUser(PropertySet.create().put(SearchSuggestionProperty.URN, Urn.forUser(456)), SEARCH_QUERY);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(view, CLICK_POSITION);

        verify(suggestionListener).onSuggestionClicked(suggestionItem);
    }


    @Test
    public void triggersPlaylistClickEventOnUserItemClicked() {
        final SuggestionItem suggestionItem = SuggestionItem.forPlaylist(PropertySet.create().put(SearchSuggestionProperty.URN, Urn.forPlaylist(789)), SEARCH_QUERY);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(view, CLICK_POSITION);

        verify(suggestionListener).onSuggestionClicked(suggestionItem);
    }

    @Test
    public void unsubscribeSuggestionListenerWhenViewDestroyed() {
        when(adapter.getItem(anyInt())).thenReturn(SuggestionItem.forSearch(SEARCH_QUERY));

        presenter.onCreate(fragmentRule.getFragment(), new Bundle());
        presenter.onDestroy(fragmentRule.getFragment());
        presenter.onItemClicked(view, CLICK_POSITION);

        verify(suggestionListener, never()).onSearchClicked(anyString());
    }

    @Test
    public void doesNotRepeatSearchWithTheSameQuery() {
        when(operations.suggestionsFor(SEARCH_QUERY)).thenReturn(Observable.<List<SuggestionItem>>empty());

        presenter.showSuggestionsFor(SEARCH_QUERY);
        presenter.getCollectionBinding().items().subscribe((Subscriber) testSubscriber);
        verify(operations).suggestionsFor(SEARCH_QUERY);
        reset(operations);

        presenter.showSuggestionsFor(SEARCH_QUERY);
        verifyZeroInteractions(operations);
    }
}
