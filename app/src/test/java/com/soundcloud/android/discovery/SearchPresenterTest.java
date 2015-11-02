package com.soundcloud.android.discovery;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class SearchPresenterTest extends AndroidUnitTest {

    private SearchPresenter presenter;

    @Mock private SuggestionsAdapter adapter;
    @Mock private SuggestionsHelper suggestionsHelper;
    @Mock private SuggestionsHelperFactory suggestionsHelperFactory;

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.search_screen);

    @Before
    public void setUp() {
        when(adapter.getViewTypeCount()).thenReturn(3);
        when(suggestionsHelperFactory.create(adapter)).thenReturn(suggestionsHelper);
        presenter = new SearchPresenter(adapter, suggestionsHelperFactory);
    }

    @Test
    public void launchesSearchSuggestion() {
        when(adapter.isSearchItem(anyInt())).thenReturn(false);

        presenter.onAttach(fragmentRule.getFragment(), fragmentRule.getActivity());
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), new Bundle());
        clickSearchItem();

        verify(suggestionsHelper).launchSuggestion(any(Context.class), anyInt());
        verifyNoMoreInteractions(suggestionsHelper);
    }

    private void clickSearchItem() {
        final View view = mock(View.class);
        when(view.getContext()).thenReturn(context());

        ListView listView = (ListView) fragmentRule.getView().findViewById(android.R.id.list);
        listView.performItemClick(view, 0, 0);
    }
}