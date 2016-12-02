package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SearchSuggestionsPresenter.SuggestionListener;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscriber;
import rx.observers.TestSubscriber;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import java.util.List;

public class SearchSuggestionsPresenterTest extends AndroidUnitTest {

    private static final String SEARCH_QUERY = "query";
    private static final int CLICK_POSITION = 1;
    private final Screen SCREEN = Screen.SEARCH_SUGGESTIONS;

    private SearchSuggestionsPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private SuggestionsAdapter adapter;
    @Mock private SearchSuggestionOperations operations;
    @Mock private SuggestionListener suggestionListener;
    @Mock private List<SuggestionItem> suggestionItems;
    @Mock private View view;
    @Mock private MixedItemClickListener.Factory clickListenerFactory;
    @Mock private MixedItemClickListener clickListener;
    @Mock private EventTracker eventTracker;
    @Captor private ArgumentCaptor<SearchEvent> searchEventCaptor;

    @Rule public FragmentRule fragmentRule = new FragmentRule(R.layout.recyclerview_with_emptyview);

    private TestSubscriber<List<SuggestionItem>> testSubscriber = new TestSubscriber<>();

    @Before
    public void setUp() {
        when(clickListenerFactory.create(eq(Screen.SEARCH_SUGGESTIONS), any(SearchQuerySourceInfo.class))).thenReturn(clickListener);
        presenter = new SearchSuggestionsPresenter(swipeRefreshAttacher, adapter, operations, clickListenerFactory, eventTracker);
        when(operations.suggestionsFor(anyString())).thenReturn(Observable.just(suggestionItems));

        presenter.setSuggestionListener(suggestionListener);
    }

    @Test
    public void triggersSearchEventOnSearchItemClicked() {
        final SuggestionItem suggestionItem = SuggestionItem.forLegacySearch(SEARCH_QUERY);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(mock(View.class), CLICK_POSITION);

        verify(suggestionListener).onSearchClicked(SEARCH_QUERY);
    }

    @Test
    public void triggersTrackClickEventOnTrackItemClicked() {
        final SuggestionItem suggestionItem = SuggestionItem.forTrack(PropertySet.create().put(SearchSuggestionProperty.URN, Urn.forTrack(123)), SEARCH_QUERY);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(view, CLICK_POSITION);

        verify(suggestionListener).onSuggestionClicked();
    }

    @Test
    public void triggersUserClickEventOnUserItemClicked() {
        final SuggestionItem suggestionItem = SuggestionItem.forUser(PropertySet.create().put(SearchSuggestionProperty.URN, Urn.forUser(456)), SEARCH_QUERY);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(view, CLICK_POSITION);

        verify(suggestionListener).onSuggestionClicked();
    }


    @Test
    public void triggersPlaylistClickEventOnUserItemClicked() {
        final SuggestionItem suggestionItem = SuggestionItem.forPlaylist(PropertySet.create().put(SearchSuggestionProperty.URN, Urn.forPlaylist(789)), SEARCH_QUERY);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(view, CLICK_POSITION);

        verify(suggestionListener).onSuggestionClicked();
    }

    @Test
    public void unsubscribeSuggestionListenerWhenViewDestroyed() {
        when(adapter.getItem(anyInt())).thenReturn(SuggestionItem.forLegacySearch(SEARCH_QUERY));

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

    @Test
    public void performsSuggestionAction() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(789);
        final SearchSuggestionItem suggestionItem = SuggestionItem.forPlaylist(PropertySet.create().put(SearchSuggestionProperty.URN, playlistUrn), SEARCH_QUERY);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(view, CLICK_POSITION);

        verify(eventTracker).trackSearch(searchEventCaptor.capture());
        verify(clickListener).onItemClick(eq(suggestionItem), any(Context.class));

        final SearchEvent capturedEvent = searchEventCaptor.getValue();
        assertThat(capturedEvent.pageName().get()).isEqualTo(SCREEN.get());
        assertThat(capturedEvent.query().get()).isEqualTo(SEARCH_QUERY);
        assertThat(capturedEvent.queryPosition().get()).isEqualTo(CLICK_POSITION);
        assertThat(capturedEvent.kind().isPresent()).isFalse();
    }
}
