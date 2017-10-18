package com.soundcloud.android.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.utils.ScTextUtils
import kotlinx.android.synthetic.main.user_detail_bio_item.view.*
import javax.inject.Inject

class UserBioRenderer
@Inject
constructor() : CellRenderer<UserBioItem> {
    override fun createItemView(parent: ViewGroup) = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_detail_bio_item, parent, false)

    override fun bindItemView(position: Int, itemView: View, items: List<UserBioItem>) = with(items[position]) {
        itemView.bio_text.text = ScTextUtils.fromHtml(bio)
    }
}
