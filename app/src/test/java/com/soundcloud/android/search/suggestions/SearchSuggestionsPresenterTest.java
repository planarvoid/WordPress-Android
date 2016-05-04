package com.soundcloud.android.search.suggestions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

public class SearchSuggestionsPresenterTest extends AndroidUnitTest {

    private static final String SEARCH_QUERY = "query";

    private SearchSuggestionsPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private SuggestionsAdapter adapter;
    @Mock private SearchSuggestionOperations operations;

    @Captor private ArgumentCaptor<SuggestionItem> captor;

    @Before
    public void setUp() {
        presenter = new SearchSuggestionsPresenter(swipeRefreshAttacher, adapter, operations);
        when(operations.suggestionsFor(anyString())).thenReturn(Observable.just(mock(SuggestionsResult.class)));
    }

    @Test
    public void test() {
        //TODO: remove this
        assertThat(true).isTrue();
    }
}
