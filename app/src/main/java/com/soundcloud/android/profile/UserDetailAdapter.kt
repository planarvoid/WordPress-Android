package com.soundcloud.android.profile

import android.view.View
import com.soundcloud.android.presentation.CellRendererBinding
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter
import com.soundcloud.android.presentation.RecyclerItemAdapter
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class UserDetailAdapter
@Inject constructor(userFollowsRenderer: UserFollowRenderer,
                    userBioRenderer: UserBioRenderer,
                    userLinksRenderer: UserLinksRenderer,
                    userLoadingRenderer: UserLoadingRenderer) :
        PagingRecyclerItemAdapter<UserDetailItem, RecyclerItemAdapter.ViewHolder>(
                CellRendererBinding(Kind.BIO.ordinal, userBioRenderer),
                CellRendererBinding(Kind.FOLLOWS.ordinal, userFollowsRenderer),
                CellRendererBinding(Kind.LINKS.ordinal, userLinksRenderer),
                CellRendererBinding(Kind.LOADING.ordinal, userLoadingRenderer)
        ) {

    override fun createViewHolder(itemView: View) = RecyclerItemAdapter.ViewHolder(itemView)

    override fun getBasicItemViewType(position: Int) = when (getItem(position)) {
        is UserBioItem -> Kind.BIO.ordinal
        is UserFollowsItem -> Kind.FOLLOWS.ordinal
        is UserLinksItem -> Kind.LINKS.ordinal
        is UserLoadingItem -> Kind.LOADING.ordinal
    }

    enum class Kind {
        BIO, FOLLOWS, LINKS, LOADING
    }

    class Factory
    @Inject constructor(val userLinksRendererFactory: UserLinksRenderer.Factory,
                        val userFollowsRendererFactory: UserFollowRenderer.Factory,
                        val userBioRenderer: UserBioRenderer,
                        val userLoadingRenderer: UserLoadingRenderer) {
        fun create(followersClickListener: PublishSubject<UserFollowsItem>,
                   followingsClickListener: PublishSubject<UserFollowsItem>,
                   linkClickListener: PublishSubject<String>): UserDetailAdapter {
            return UserDetailAdapter(userFollowsRendererFactory.create(followersClickListener, followingsClickListener),
                                     userBioRenderer,
                                     userLinksRendererFactory.create(linkClickListener),
                                     userLoadingRenderer)
        }
    }
}
