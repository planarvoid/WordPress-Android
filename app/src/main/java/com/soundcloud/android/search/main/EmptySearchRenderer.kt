package com.soundcloud.android.search.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.presentation.CellRenderer

class EmptySearchRenderer : CellRenderer<SearchItemViewModel> {

    override fun bindItemView(position: Int, itemView: View?, items: MutableList<SearchItemViewModel>?) {
        // no-op
    }

    override fun createItemView(viewGroup: ViewGroup): View = LayoutInflater.from(viewGroup.context).inflate(R.layout.emptyview_search_tab, viewGroup, false)

}
