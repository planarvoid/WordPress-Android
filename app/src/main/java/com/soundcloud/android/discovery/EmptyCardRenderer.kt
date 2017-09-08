package com.soundcloud.android.discovery

import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.utils.EmptyThrowable
import com.soundcloud.android.utils.ErrorUtils.emptyViewStatusFromError
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.view.EmptyView
import com.soundcloud.android.view.EmptyViewBuilder
import javax.inject.Inject

@OpenForTesting
class EmptyCardRenderer
@Inject
internal constructor() : CellRenderer<DiscoveryCardViewModel.EmptyCard> {

    override fun createItemView(viewGroup: ViewGroup): View {
        val context = viewGroup.context
        return EmptyViewBuilder()
                .withMessageText(context.getString(R.string.discovery_empty))
                .withPadding(R.dimen.empty_card_left_padding,
                             R.dimen.empty_card_top_padding,
                             R.dimen.empty_card_right_padding,
                             R.dimen.empty_card_bottom_padding)
                .build(context)
    }

    override fun bindItemView(position: Int, view: View, list: List<DiscoveryCardViewModel.EmptyCard>) {
        val (throwable) = list[position]

        val status = emptyViewStatusFromError(throwable ?: EmptyThrowable())
        if (view is EmptyView) {
            view.setStatus(status)
        }
    }
}
