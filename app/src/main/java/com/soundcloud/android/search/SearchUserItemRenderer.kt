package com.soundcloud.android.search

import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import com.soundcloud.android.R
import com.soundcloud.android.analytics.EngagementsTracking
import com.soundcloud.android.analytics.ScreenProvider
import com.soundcloud.android.associations.FollowingOperations
import com.soundcloud.android.events.EventContextMetadata
import com.soundcloud.android.events.Module
import com.soundcloud.android.model.Urn
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.rx.observers.DefaultDisposableCompletableObserver
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.view.adapters.UserItemRenderer
import com.soundcloud.java.optional.Optional
import javax.inject.Inject

@OpenForTesting
class SearchUserItemRenderer
@Inject
constructor(private val userItemRenderer: UserItemRenderer,
            private val followingOperations: FollowingOperations,
            private val engagementsTracking: EngagementsTracking,
            private val screenProvider: ScreenProvider) : CellRenderer<SearchUserItem> {

    override fun createItemView(parent: ViewGroup): View = userItemRenderer.createItemView(parent)

    override fun bindItemView(position: Int, itemView: View, items: List<SearchUserItem>) {
        items.getOrNull(position)?.let {
            this.userItemRenderer.bindItemView(itemView, it.userItem)
            this.setupFollowToggle(itemView, it, position)
        }
    }

    private fun setupFollowToggle(itemView: View, user: SearchUserItem, position: Int) {
        val toggleFollow = itemView.findViewById<ToggleButton>(R.id.toggle_btn_follow)
        toggleFollow.visibility = View.VISIBLE
        toggleFollow.isChecked = user.userItem.isFollowedByMe
        toggleFollow.setOnClickListener {
            val screen = screenProvider.lastScreen.get()

            followingOperations.toggleFollowing(user.urn, toggleFollow.isChecked).subscribe(DefaultDisposableCompletableObserver())

            engagementsTracking.followUserUrn(user.urn,
                                              toggleFollow.isChecked,
                                              getEventContextMetadata(position, screen, user.queryUrn))
        }
    }

    private fun getEventContextMetadata(position: Int, screen: String, queryUrn: Optional<Urn>): EventContextMetadata {
        return EventContextMetadata.builder()
                .module(Module.create(screen, position))
                .pageName(screen)
                .queryUrn(queryUrn)
                .build()
    }
}
