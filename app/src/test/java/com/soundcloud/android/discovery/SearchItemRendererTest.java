package com.soundcloud.android.discovery;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
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

    @Mock private SearchController searchController;
    @Mock private AutoCompleteTextView searchView;
    @Mock private SearchItemRenderer.OnSearchListener listener;

    @Before
    public void setUp() {
        renderer = new SearchItemRenderer(searchController);
    }

    @Test
    public void delegatesBindSearchViewToController() {
        final View itemView = mock(View.class);
        when(itemView.findViewById(R.id.search)).thenReturn(searchView);

        renderer.bindItemView(0, itemView, Collections.EMPTY_LIST);

        verify(searchController).bindSearchView(searchView, renderer);
    }

    @Test
    public void performsTextSearch() {
        final Context context = context();
        final String query = "query";

        renderer.setOnSearchListener(listener);
        renderer.performTextSearch(context, query);

        verify(listener).onSearchTextPerformed(context, query);
    }

    @Test
    public void launchesSearchSuggestion() {
        final Context context = context();
        final Urn trackUrn = Urn.forTrack(12L);
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(trackUrn);

        renderer.setOnSearchListener(listener);
        renderer.launchSearchSuggestion(context, trackUrn, searchQuerySourceInfo, Uri.EMPTY);

        verify(listener).onLaunchSearchSuggestion(context, trackUrn, searchQuerySourceInfo, Uri.EMPTY);
    }
}