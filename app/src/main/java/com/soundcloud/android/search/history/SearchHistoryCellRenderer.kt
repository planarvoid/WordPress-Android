package com.soundcloud.android.search.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.utils.ViewUtils
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.autocompletion_item.view.*
import javax.inject.Inject

internal class SearchHistoryCellRenderer
@Inject
constructor(private val itemClickListener: PublishSubject<SearchHistoryItem>,
            private val autocompleteArrowClickListener: PublishSubject<SearchHistoryItem>) : CellRenderer<SearchHistoryItem> {

    override fun createItemView(parent: ViewGroup): View {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_history_item, parent, false)
        ViewUtils.extendTouchArea(view.arrow_icon, R.dimen.search_suggestion_arrow_touch_expansion)
        return view
    }

    override fun bindItemView(position: Int, itemView: View, items: List<SearchHistoryItem>) {
        itemView.search_title.text = items[position].searchTerm
        itemView.setOnClickListener { itemClickListener.onNext(items[position]) }
        itemView.arrow_icon.setOnClickListener { autocompleteArrowClickListener.onNext(items[position]) }
    }

    internal class Factory
    @Inject
    internal constructor() {
        fun create(itemClickListener: PublishSubject<SearchHistoryItem>,
                   autocompleteArrowClickListener: PublishSubject<SearchHistoryItem>) =
                SearchHistoryCellRenderer(itemClickListener, autocompleteArrowClickListener)
    }

}
