package com.soundcloud.android.profile

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.view.CustomFontTextView
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.user_detail_links_item.view.*
import kotlinx.android.synthetic.main.user_info_social_media_link.view.*
import javax.inject.Inject

class UserLinksRenderer(val linkClickListener: PublishSubject<String>) : CellRenderer<UserLinksItem> {

    override fun createItemView(parent: ViewGroup) = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_detail_links_item, parent, false)

    @Suppress("UnsafeCast")
    override fun bindItemView(position: Int, itemView: View, items: List<UserLinksItem>) {
        items[position].socialMediaLinks.forEach { link ->
            val view = LayoutInflater.from(itemView.context).inflate(R.layout.user_info_social_media_link, itemView.links_container, false) as CustomFontTextView

            view.movementMethod = LinkMovementMethod.getInstance()
            view.text = link.displayName()
            view.social_link.social_link.setCompoundDrawablesWithIntrinsicBounds(link.icon(itemView.context), null, null, null)
            view.setOnClickListener { linkClickListener.onNext(link.url) }

            itemView.links_container.addView(view)
        }
    }

    class Factory
    @Inject constructor() {
        fun create(linkClickListener: PublishSubject<String>) = UserLinksRenderer(linkClickListener)
    }
}
