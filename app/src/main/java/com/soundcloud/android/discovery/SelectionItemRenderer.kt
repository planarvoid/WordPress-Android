package com.soundcloud.android.discovery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.soundcloud.android.R
import com.soundcloud.android.image.ImageOperations
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.java.optional.Optional
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.carousel_playlist_item.view.*
import kotlinx.android.synthetic.main.discovery_carousel_playlist_item_fixed_width.view.*
import javax.inject.Inject

class SelectionItemRenderer(private val imageOperations: ImageOperations,
                            private val selectionItemClickListener: PublishSubject<SelectionItemViewModel>) : CellRenderer<SelectionItemViewModel> {

    override fun createItemView(parent: ViewGroup): View {
        return LayoutInflater.from(parent.context)
                .inflate(R.layout.discovery_carousel_playlist_item_fixed_width, parent, false)
    }

    override fun bindItemView(position: Int, view: View, list: List<SelectionItemViewModel>) {
        val selectionItem = list[position]

        bindImage(view, selectionItem)
        bindText(view.title, selectionItem.shortTitle)
        bindText(view.secondary_text, selectionItem.shortSubtitle)
        bindText(view.track_count, selectionItem.count?.toString())
        bindOverflowMenu(view)
        bindClickHandling(view, selectionItem)
    }

    private fun bindOverflowMenu(view: View) {
        // not MVP, since there are no additional actions to take after opening the overflow menu
        view.overflow_button.visibility = View.GONE
    }

    private fun bindImage(view: View, selectionItem: SelectionItemViewModel) {
        val styledImageView = view.artwork
        styledImageView.showWithPlaceholder(Optional.fromNullable(selectionItem.artworkUrlTemplate),
                                            Optional.fromNullable(selectionItem.artworkStyle),
                                            Optional.fromNullable(selectionItem.urn),
                                            imageOperations)
    }

    private fun bindText(textView: TextView, text: String?) {
        if (text != null) {
            textView.text = text
            textView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.GONE
        }
    }

    private fun bindClickHandling(view: View, selectionItem: SelectionItemViewModel) {
        view.setOnClickListener { selectionItemClickListener.onNext(selectionItem) }
    }

    data class Factory
    @Inject
    constructor(private val imageOperations: ImageOperations) {
        fun create(selectionItemClickListener: PublishSubject<SelectionItemViewModel>) = SelectionItemRenderer(imageOperations, selectionItemClickListener)
    }
}
