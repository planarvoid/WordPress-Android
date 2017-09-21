package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SearchSuggestionsPresenter.SuggestionListener;
import static org.assertj.core.api.Java6Assertions.assertThat;
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
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.os.Bundle;
import android.view.View;

import java.util.List;

public class SearchSuggestionsPresenterTest extends AndroidUnitTest {

    private static final String API_QUERY = "api_query";
    private static final String USER_QUERY = "user_query";
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

    @Before
    public void setUp() {
        when(clickListenerFactory.create(eq(Screen.SEARCH_SUGGESTIONS), any(SearchQuerySourceInfo.class))).thenReturn(clickListener);
        presenter = new SearchSuggestionsPresenter(swipeRefreshAttacher, adapter, operations, clickListenerFactory, eventTracker);
        when(operations.suggestionsFor(anyString())).thenReturn(Observable.just(suggestionItems));
        when(view.getContext()).thenReturn(context());

        presenter.setSuggestionListener(suggestionListener);
    }

    @Test
    public void triggersSearchEventOnSearchItemArrowClicked() {
        Optional<Urn> queryUrn = Optional.of(Urn.forTrack(123L));
        final SuggestionItem suggestionItem = SuggestionItem.forAutocompletion(Autocompletion.create(API_QUERY, USER_QUERY), USER_QUERY, queryUrn);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.handleClick(USER_QUERY, API_QUERY, queryUrn, CLICK_POSITION);

        verify(suggestionListener).onAutocompleteArrowClicked(USER_QUERY, API_QUERY, queryUrn, Optional.of(CLICK_POSITION));
    }

    @Test
    public void triggersSearchEventOnSearchItemClicked() {
        final SuggestionItem suggestionItem = SuggestionItem.forAutocompletion(Autocompletion.create(API_QUERY, USER_QUERY), USER_QUERY, Optional.absent());
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(mock(View.class), CLICK_POSITION);

        verify(suggestionListener).onAutocompleteClicked(API_QUERY, USER_QUERY, USER_QUERY, Optional.absent(), CLICK_POSITION);
    }

    @Test
    public void triggersTrackClickEventOnTrackItemClicked() {
        final SuggestionItem suggestionItem = SearchSuggestionItem.forTrack(Urn.forTrack(123), Optional.absent(), API_QUERY, Optional.absent(), API_QUERY);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(view, CLICK_POSITION);

        verify(suggestionListener).onSuggestionClicked(API_QUERY);
    }

    @Test
    public void triggersUserClickEventOnUserItemClicked() {
        final SuggestionItem suggestionItem = SearchSuggestionItem.forUser(Urn.forUser(456), Optional.absent(), API_QUERY, Optional.absent(), API_QUERY, false);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(view, CLICK_POSITION);

        verify(suggestionListener).onSuggestionClicked(API_QUERY);
    }


    @Test
    public void triggersPlaylistClickEventOnUserItemClicked() {
        final SuggestionItem suggestionItem = SearchSuggestionItem.forPlaylist(Urn.forPlaylist(789), Optional.absent(), API_QUERY, Optional.absent(), API_QUERY);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(view, CLICK_POSITION);

        verify(suggestionListener).onSuggestionClicked(API_QUERY);
    }

    @Test
    public void unsubscribeSuggestionListenerWhenViewDestroyed() {
        final SuggestionItem suggestionItem = SuggestionItem.forAutocompletion(Autocompletion.create(API_QUERY, USER_QUERY), USER_QUERY, Optional.absent());
        when(adapter.getItem(anyInt())).thenReturn(suggestionItem);

        presenter.onCreate(fragmentRule.getFragment(), new Bundle());
        presenter.onDestroy(fragmentRule.getFragment());
        presenter.onItemClicked(view, CLICK_POSITION);

        verify(suggestionListener, never()).onSearchClicked(anyString(), anyString());
    }

    @Test
    public void sendsTheUserQueryWhenClicked() throws Exception {
        final SuggestionItem suggestionItem = SearchSuggestionItem.forPlaylist(Urn.forPlaylist(789), Optional.absent(), API_QUERY, Optional.absent(), API_QUERY);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(view, CLICK_POSITION);

        verify(suggestionListener).onSuggestionClicked(suggestionItem.userQuery());
    }

    @Test
    public void doesNotRepeatSearchWithTheSameQuery() {
        when(operations.suggestionsFor(API_QUERY)).thenReturn(Observable.empty());

        presenter.showSuggestionsFor(API_QUERY);
        presenter.getCollectionBinding().items().test();
        verify(operations).suggestionsFor(API_QUERY);
        reset(operations);

        presenter.showSuggestionsFor(API_QUERY);
        verifyZeroInteractions(operations);
    }

    @Test
    public void performsSuggestionAction() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(789);
        final SearchSuggestionItem suggestionItem = SearchSuggestionItem.forPlaylist(playlistUrn, Optional.absent(), API_QUERY, Optional.absent(), API_QUERY);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(view, CLICK_POSITION);

        verify(eventTracker).trackSearch(searchEventCaptor.capture());
        verify(clickListener).onItemClick(suggestionItem, context());

        final SearchEvent capturedEvent = searchEventCaptor.getValue();
        assertThat(capturedEvent.pageName().get()).isEqualTo(SCREEN.get());
        assertThat(capturedEvent.query().get()).isEqualTo(API_QUERY);
        assertThat(capturedEvent.queryPosition().get()).isEqualTo(CLICK_POSITION);
        assertThat(capturedEvent.kind().isPresent()).isFalse();
    }
}
