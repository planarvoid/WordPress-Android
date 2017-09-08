package com.soundcloud.android.discovery

import android.view.View
import android.widget.LinearLayout
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.AndroidUnitTest
import kotlinx.android.synthetic.main.discovery_multiple_content_selection_card.view.*
import org.assertj.android.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify

class MultipleContentSelectionCardRendererTest : AndroidUnitTest() {

    @Mock internal lateinit var selectionItemAdapterFactory: SelectionItemAdapter.Factory
    @Mock internal lateinit var adapter: SelectionItemAdapter
    private val card = DiscoveryFixtures.multiContentSelectionCardViewModel()
    private lateinit var itemView: View
    private lateinit var renderer: MultipleContentSelectionCardRenderer

    @Before
    fun setUp() {

        renderer = MultipleContentSelectionCardRenderer(selectionItemAdapterFactory)

        whenever(selectionItemAdapterFactory.create(renderer.selectionItemInCardClickListener)).thenReturn(adapter)
        whenever(adapter.selectionUrn).thenReturn(Urn.NOT_SET)

        itemView = renderer.createItemView(LinearLayout(AndroidUnitTest.context()))
    }

    @Test
    fun bindsTitleWhenPresent() {
        val cardWithTitle = card.copy(title = "title")
        val title = itemView.selection_title

        renderer.bindItemView(0, itemView, listOf(cardWithTitle))

        assertThat(title).isVisible
    }

    @Test
    fun doesNotBindTitleWhenNotPresent() {
        val title = itemView.selection_title
        val cardWithoutTitle = card.copy(title = null)

        renderer.bindItemView(0, itemView, listOf(cardWithoutTitle))

        assertThat(title).isNotVisible
    }

    @Test
    fun bindsDescriptionWhenPresent() {
        val cardWithDescription = card.copy(description = "description")
        val description = itemView.selection_description

        renderer.bindItemView(0, itemView, listOf(cardWithDescription))

        assertThat(description).isVisible
    }

    @Test
    fun doesNotBindDescriptionWhenNotPresent() {
        val description = itemView.selection_description

        val cardWithoutDescription = card.copy(description = null)

        renderer.bindItemView(0, itemView, listOf(cardWithoutDescription))

        assertThat(description).isNotVisible
    }

    @Test
    fun bindsTheSelectionItemsInTheCarousel() {
        renderer.bindItemView(0, itemView, listOf<DiscoveryCardViewModel.MultipleContentSelectionCard>(card))

        verify<SelectionItemAdapter>(adapter).updateSelection(card)
    }
}
