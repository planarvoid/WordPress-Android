package com.soundcloud.android.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.utils.OpenForTesting
import kotlinx.android.synthetic.main.search_item.view.*

@OpenForTesting
class SearchItemRenderer<T> : CellRenderer<T> {

    interface SearchListener {
        fun onSearchClicked(context: Context)
    }

    private var searchListener: SearchListener? = null

    override fun createItemView(viewGroup: ViewGroup): View = LayoutInflater.from(viewGroup.context).inflate(R.layout.search_item, viewGroup, false)

    override fun bindItemView(position: Int, itemView: View, unused: List<T>) {
        itemView.search_item.setOnClickListener { searchListener?.onSearchClicked(it.context) }
    }

    fun setSearchListener(searchListener: SearchListener) {
        this.searchListener = searchListener
    }
}
