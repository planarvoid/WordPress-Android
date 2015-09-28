package com.soundcloud.android.discovery;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import android.view.View;
import android.widget.AutoCompleteTextView;

import java.util.Collections;

public class SearchItemRendererTest extends AndroidUnitTest {

    private SearchItemRenderer renderer;

    @Mock private SuggestionsAdapter adapter;
    @Mock private View itemView;
    @Mock private SearchItemRenderer.SearchListener listener;

    private AutoCompleteTextView searchView;

    @Before
    public void setUp() {
        renderer = new SearchItemRenderer(adapter);
        searchView = new AutoCompleteTextView(context());

        when(itemView.findViewById(anyInt())).thenReturn(searchView);
    }

    @Test
    public void performsTextSearch() {
        when(adapter.isSearchItem(anyInt())).thenReturn(true);

        renderer.setSearchListener(listener);
        renderer.bindItemView(0, itemView, Collections.EMPTY_LIST);
        clickSearchItem();

        verify(listener).onSearchTextPerformed(any(Context.class), anyString());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void launchesSearchSuggestion() {
        when(adapter.isSearchItem(anyInt())).thenReturn(false);
        when(adapter.getItemIntentData(anyInt())).thenReturn(Uri.EMPTY);
        when(adapter.getQueryUrn(anyInt())).thenReturn(Urn.NOT_SET);

        renderer.setSearchListener(listener);
        renderer.bindItemView(0, itemView, Collections.EMPTY_LIST);
        clickSearchItem();

        verify(listener).onLaunchSearchSuggestion(any(Context.class), any(Urn.class), any(SearchQuerySourceInfo.class), any(Uri.class));
        verifyNoMoreInteractions(listener);
    }

    private void clickSearchItem() {
        searchView.getOnItemClickListener().onItemClick(null, null, 0, 0);
    }
}