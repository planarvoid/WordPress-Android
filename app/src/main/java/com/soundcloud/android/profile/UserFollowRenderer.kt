package com.soundcloud.android.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.util.CondensedNumberFormatter
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.user_detail_follows_item.view.*
import javax.inject.Inject

class UserFollowRenderer
@Inject
constructor(val condensedNumberFormatter: CondensedNumberFormatter,
            val followersClickListener: PublishSubject<UserFollowsItem>,
            val followingsClickListener: PublishSubject<UserFollowsItem>) : CellRenderer<UserFollowsItem> {

    override fun createItemView(parent: ViewGroup) = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_detail_follows_item, parent, false)

    override fun bindItemView(position: Int, itemView: View, items: List<UserFollowsItem>) = with(items[position]) {
        followersCount?.let { itemView.followers_count.text = condensedNumberFormatter.format(it.toLong()) }
        followingCount?.let { itemView.followings_count.text = condensedNumberFormatter.format(it.toLong()) }
        itemView.view_followers.setOnClickListener { followersClickListener.onNext(this) }
        itemView.view_following.setOnClickListener { followingsClickListener.onNext(this) }
    }

    class Factory
    @Inject constructor(val condensedNumberFormatter: CondensedNumberFormatter) {
        fun create(followersClickListener: PublishSubject<UserFollowsItem>,
                   followingsClickListener: PublishSubject<UserFollowsItem>) = UserFollowRenderer(
                condensedNumberFormatter,
                followersClickListener,
                followingsClickListener)
    }
}
