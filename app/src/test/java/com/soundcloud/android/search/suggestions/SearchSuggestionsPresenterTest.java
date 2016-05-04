package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SearchSuggestionsPresenter.SuggestionListener;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PropertySetSource;
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

import java.util.Collections;
import java.util.List;

public class SearchSuggestionsPresenterTest extends AndroidUnitTest {

    private static final String SEARCH_QUERY = "query";
    private static final int CLICK_POSITION = 1;

    private SearchSuggestionsPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private SuggestionsAdapter adapter;
    @Mock private SearchSuggestionOperations operations;
    @Mock private SuggestionListener suggestionListener;

    @Rule public FragmentRule fragmentRule = new FragmentRule(R.layout.recyclerview_with_emptyview);

    private TestSubscriber<List<SuggestionItem>> testSubscriber = new TestSubscriber<>();

    @Before
    public void setUp() {
        presenter = new SearchSuggestionsPresenter(swipeRefreshAttacher, adapter, operations);
        when(operations.suggestionsFor(anyString())).thenReturn(Observable.just(mock(SuggestionsResult.class)));
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
        final Urn trackUrn = Urn.forTrack(123);
        final SuggestionItem suggestionItem = mock(SuggestionItem.class);
        when(suggestionItem.getKind()).thenReturn(SuggestionItem.Kind.TrackItem);
        when(suggestionItem.getUrn()).thenReturn(trackUrn);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(mock(View.class), CLICK_POSITION);

        verify(suggestionListener).onTrackClicked(trackUrn);
    }

    @Test
    public void triggersUserClickEventOnUserItemClicked() {
        final Urn trackUrn = Urn.forUser(123);
        final SuggestionItem suggestionItem = mock(SuggestionItem.class);
        when(suggestionItem.getKind()).thenReturn(SuggestionItem.Kind.UserItem);
        when(suggestionItem.getUrn()).thenReturn(trackUrn);
        when(adapter.getItem(CLICK_POSITION)).thenReturn(suggestionItem);

        presenter.onItemClicked(mock(View.class), CLICK_POSITION);

        verify(suggestionListener).onUserClicked(trackUrn);
    }

    @Test
    public void unsubscribeSuggestionListenerWhenViewDestroyed() {
        when(adapter.getItem(anyInt())).thenReturn(SuggestionItem.forSearch(SEARCH_QUERY));

        presenter.onCreate(fragmentRule.getFragment(), new Bundle());
        presenter.onDestroy(fragmentRule.getFragment());
        presenter.onItemClicked(mock(View.class), CLICK_POSITION);

        verify(suggestionListener, never()).onSearchClicked(anyString());
    }

    @Test
    public void shouldContainSearchItemOnFirstPosition() {
        when(operations.suggestionsFor(SEARCH_QUERY)).thenReturn(Observable.just(SuggestionsResult.emptyLocal()));

        presenter.showSuggestionsFor(SEARCH_QUERY);
        presenter.getCollectionBinding().items().subscribe((Subscriber)testSubscriber);

        final SuggestionItem firstSuggestionItem = testSubscriber.getOnNextEvents().get(0).get(0);
        assertThat(firstSuggestionItem.getKind()).isEqualTo(SuggestionItem.Kind.SearchItem);
        testSubscriber.assertCompleted();
    }

    @Test
    public void shouldShowLocalAndRemoteSuggestions() {
        final PropertySet localSuggestion = PropertySet.create();
        localSuggestion.put(SearchSuggestionProperty.URN, Urn.forTrack(123));
        final List<PropertySet> localItems = Collections.singletonList(localSuggestion);
        final SuggestionsResult localSuggestionsResult = SuggestionsResult.localFromPropertySets(localItems);

        final PropertySet remoteSuggestion = PropertySet.create();
        remoteSuggestion.put(SearchSuggestionProperty.URN, Urn.forUser(123));
        final PropertySetSource propertySetSource = new RemoteSuggestions(remoteSuggestion);
        final List<PropertySetSource> remoteItems = Collections.singletonList(propertySetSource);
        final SuggestionsResult remoteSuggestionsResult = SuggestionsResult.remoteFromPropertySetSource(remoteItems);

        when(operations.suggestionsFor(SEARCH_QUERY)).thenReturn(Observable.just(localSuggestionsResult, remoteSuggestionsResult));

        presenter.showSuggestionsFor(SEARCH_QUERY);
        presenter.getCollectionBinding().items().subscribe((Subscriber)testSubscriber);

        final SuggestionItem firstItem = testSubscriber.getOnNextEvents().get(0).get(0);
        final SuggestionItem secondItem = testSubscriber.getOnNextEvents().get(0).get(1);
        final SuggestionItem thirdItem = testSubscriber.getOnNextEvents().get(0).get(2);

        assertThat(firstItem.getKind()).isEqualTo(SuggestionItem.Kind.SearchItem);
        assertThat(secondItem.getKind()).isEqualTo(SuggestionItem.Kind.TrackItem);
        assertThat(thirdItem.getKind()).isEqualTo(SuggestionItem.Kind.UserItem);
        testSubscriber.assertCompleted();
    }

    private static class RemoteSuggestions implements PropertySetSource {
        private PropertySet source;

        private RemoteSuggestions(PropertySet source) {
            this.source = source;
        }

        @Override
        public PropertySet toPropertySet() {
            return source;
        }
    }
}
