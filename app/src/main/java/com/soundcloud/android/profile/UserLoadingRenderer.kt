package com.soundcloud.android.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.presentation.CellRenderer
import javax.inject.Inject

class UserLoadingRenderer
@Inject
constructor() : CellRenderer<UserLoadingItem> {
    override fun createItemView(parent: ViewGroup) = LayoutInflater.from(parent.context).inflate(R.layout.user_detail_loading_item, parent, false)

    override fun bindItemView(position: Int, itemView: View, items: List<UserLoadingItem>) = Unit
}
