package com.soundcloud.android.discovery;


import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.search.SearchItemRenderer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DiscoveryAdapterTest extends AndroidUnitTest {

    @Mock SearchItemRenderer searchItemRenderer;
    @Mock SingleSelectionContentCardRenderer singleSelectionContentCardRenderer;
    @Mock MultipleContentSelectionCardRenderer multipleContentSelectionCardRenderer;
    @Mock EmptyCardRenderer emptyCardRenderer;
    @Mock DiscoveryCard.MultipleContentSelectionCard multipleContentSelectionCard;
    @Mock DiscoveryCard.SingleContentSelectionCard singleContentSelectionCard;

    private DiscoveryAdapter adapter;
    private DiscoveryCard searchItem = DiscoveryCard.forSearchItem();

    @Before
    public void setUp() {
        adapter = new DiscoveryAdapter(searchItemRenderer, singleSelectionContentCardRenderer, multipleContentSelectionCardRenderer, emptyCardRenderer);
    }

    @Test
    public void rendersCorrectViewTypes() {
        when(singleContentSelectionCard.kind()).thenReturn(DiscoveryCard.Kind.SINGLE_CONTENT_SELECTION_CARD);
        when(multipleContentSelectionCard.kind()).thenReturn(DiscoveryCard.Kind.MULTIPLE_CONTENT_SELECTION_CARD);

        adapter.onNext(asList(searchItem, singleContentSelectionCard, multipleContentSelectionCard));

        assertThat(adapter.getBasicItemViewType(0)).isEqualTo(DiscoveryCard.Kind.SEARCH_ITEM.ordinal());
        assertThat(adapter.getBasicItemViewType(1)).isEqualTo(DiscoveryCard.Kind.SINGLE_CONTENT_SELECTION_CARD.ordinal());
        assertThat(adapter.getBasicItemViewType(2)).isEqualTo(DiscoveryCard.Kind.MULTIPLE_CONTENT_SELECTION_CARD.ordinal());
    }

    @Test
    public void setsSearchListener() {
        final SearchItemRenderer.SearchListener searchListener = mock(SearchItemRenderer.SearchListener.class);

        adapter.setSearchListener(searchListener);

        verify(searchItemRenderer).setSearchListener(searchListener);
    }
}
