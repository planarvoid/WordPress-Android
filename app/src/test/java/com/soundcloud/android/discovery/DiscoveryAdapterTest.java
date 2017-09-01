package com.soundcloud.android.discovery;


import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.search.SearchItemRenderer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DiscoveryAdapterTest extends AndroidUnitTest {

    @Mock SearchItemRenderer<DiscoveryCardViewModel> searchItemRenderer;
    @Mock SingleSelectionContentCardRenderer singleSelectionContentCardRenderer;
    @Mock MultipleContentSelectionCardRenderer multipleContentSelectionCardRenderer;
    @Mock EmptyCardRenderer emptyCardRenderer;
    @Mock SearchItemRenderer.SearchListener searchListener;

    final DiscoveryCardViewModel.MultipleContentSelectionCard multipleContentSelectionCard = DiscoveryFixtures.multiContentSelectionCardViewModel();
    final DiscoveryCardViewModel.SingleContentSelectionCard singleContentSelectionCard = DiscoveryFixtures.singleContentSelectionCardViewModel();

    private DiscoveryAdapter adapter;
    private DiscoveryCardViewModel searchItem = DiscoveryCardViewModel.SearchCard.INSTANCE;

    @Before
    public void setUp() {
        adapter = new DiscoveryAdapter(searchItemRenderer,
                                       singleSelectionContentCardRenderer,
                                       multipleContentSelectionCardRenderer,
                                       emptyCardRenderer,
                                       searchListener);
    }

    @Test
    public void rendersCorrectViewTypes() {
        adapter.onNext(asList(searchItem, singleContentSelectionCard, multipleContentSelectionCard));

        assertThat(adapter.getBasicItemViewType(0)).isEqualTo(DiscoveryCard.Kind.SEARCH_ITEM.ordinal());
        assertThat(adapter.getBasicItemViewType(1)).isEqualTo(DiscoveryCard.Kind.SINGLE_CONTENT_SELECTION_CARD.ordinal());
        assertThat(adapter.getBasicItemViewType(2)).isEqualTo(DiscoveryCard.Kind.MULTIPLE_CONTENT_SELECTION_CARD.ordinal());
    }
}
