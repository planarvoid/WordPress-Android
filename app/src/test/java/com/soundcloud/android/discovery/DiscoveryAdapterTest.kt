package com.soundcloud.android.discovery

import com.soundcloud.android.discovery.DiscoveryAdapter.Kind
import com.soundcloud.android.search.SearchItemRenderer
import com.soundcloud.android.testsupport.AndroidUnitTest
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import java.util.Arrays.asList

class DiscoveryAdapterTest : AndroidUnitTest() {

    @Mock internal lateinit var searchItemRenderer: SearchItemRenderer<DiscoveryCardViewModel>
    @Mock internal lateinit var singleSelectionContentCardRenderer: SingleSelectionContentCardRenderer
    @Mock internal lateinit var multipleContentSelectionCardRenderer: MultipleContentSelectionCardRenderer
    @Mock internal lateinit var emptyCardRenderer: EmptyCardRenderer
    @Mock internal lateinit var searchListener: SearchItemRenderer.SearchListener

    private val multipleContentSelectionCard = DiscoveryFixtures.multiContentSelectionCardViewModel()
    private val singleContentSelectionCard = DiscoveryFixtures.singleContentSelectionCardViewModel()

    private lateinit var adapter: DiscoveryAdapter
    private val searchItem = DiscoveryCardViewModel.SearchCard

    @Before
    fun setUp() {
        adapter = DiscoveryAdapter(searchItemRenderer,
                                   singleSelectionContentCardRenderer,
                                   multipleContentSelectionCardRenderer,
                                   emptyCardRenderer,
                                   searchListener)
    }

    @Test
    fun rendersCorrectViewTypes() {
        adapter.onNext(asList(searchItem, singleContentSelectionCard, multipleContentSelectionCard))

        assertThat(adapter.getBasicItemViewType(0)).isEqualTo(Kind.SEARCH_ITEM.ordinal)
        assertThat(adapter.getBasicItemViewType(1)).isEqualTo(Kind.SINGLE_CONTENT_SELECTION_CARD.ordinal)
        assertThat(adapter.getBasicItemViewType(2)).isEqualTo(Kind.MULTIPLE_CONTENT_SELECTION_CARD.ordinal)
    }
}
