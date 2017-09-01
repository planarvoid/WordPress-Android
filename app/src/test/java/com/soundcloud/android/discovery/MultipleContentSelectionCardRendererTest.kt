package com.soundcloud.android.discovery

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.R
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.java.optional.Optional
import org.assertj.android.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.verify

class MultipleContentSelectionCardRendererTest : AndroidUnitTest() {

    @Mock internal lateinit var selectionItemAdapterFactory: SelectionItemAdapterFactory
    @Mock internal lateinit var adapter: SelectionItemAdapter
    private val card = DiscoveryFixtures.multiContentSelectionCardViewModel()
    private lateinit var itemView: View
    private lateinit var renderer: MultipleContentSelectionCardRenderer

    @Before
    fun setUp() {
        whenever(selectionItemAdapterFactory.create(any())).thenReturn(adapter)
        whenever(adapter.selectionUrn()).thenReturn(Optional.of(Urn.NOT_SET))

        renderer = MultipleContentSelectionCardRenderer(selectionItemAdapterFactory)

        itemView = renderer.createItemView(LinearLayout(AndroidUnitTest.context()))
    }

    @Test
    fun bindsTitleWhenPresent() {
        val cardWithTitle = card.copy(title = Optional.of("title"))
        val title = itemView.findViewById<View>(R.id.selection_title) as TextView

        renderer.bindItemView(0, itemView, listOf<DiscoveryCardViewModel.MultipleContentSelectionCard>(cardWithTitle))

        assertThat(title).isVisible
    }

    @Test
    fun doesNotBindTitleWhenNotPresent() {
        val title = itemView.findViewById<View>(R.id.selection_title) as TextView
        val cardWithoutTitle = card.copy(title = Optional.absent())

        renderer.bindItemView(0, itemView, listOf<DiscoveryCardViewModel.MultipleContentSelectionCard>(cardWithoutTitle))

        assertThat(title).isNotVisible
    }

    @Test
    fun bindsDescriptionWhenPresent() {
        val cardWithDescription = card.copy(description = Optional.of("description"))
        val description = itemView.findViewById<View>(R.id.selection_description) as TextView

        renderer.bindItemView(0, itemView, listOf<DiscoveryCardViewModel.MultipleContentSelectionCard>(cardWithDescription))

        assertThat(description).isVisible
    }

    @Test
    fun doesNotBindDescriptionWhenNotPresent() {
        val description = itemView.findViewById<View>(R.id.selection_description) as TextView

        val cardWithoutDescription = card.copy(description = Optional.absent())

        renderer.bindItemView(0, itemView, listOf<DiscoveryCardViewModel.MultipleContentSelectionCard>(cardWithoutDescription))

        assertThat(description).isNotVisible
    }

    @Test
    fun bindsTheSelectionItemsInTheCarousel() {
        renderer.bindItemView(0, itemView, listOf<DiscoveryCardViewModel.MultipleContentSelectionCard>(card))

        verify<SelectionItemAdapter>(adapter).updateSelection(card)
    }
}
