package com.soundcloud.android.discovery;


import static com.soundcloud.android.utils.ErrorUtils.emptyViewStatusFromError;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.utils.EmptyThrowable;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.EmptyViewBuilder;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class EmptyCardRenderer implements CellRenderer<DiscoveryCard.EmptyCard> {

    @Inject
    EmptyCardRenderer() {

    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        Context context = viewGroup.getContext();
        return new EmptyViewBuilder()
                .withMessageText(context.getString(R.string.discovery_empty))
                .withPadding(R.dimen.empty_card_left_padding,
                             R.dimen.empty_card_top_padding,
                             R.dimen.empty_card_right_padding,
                             R.dimen.empty_card_bottom_padding)
                .build(context);
    }

    @Override
    public void bindItemView(int position, View view, List<DiscoveryCard.EmptyCard> list) {
        final DiscoveryCard.EmptyCard emptyCard = list.get(position);

        final EmptyView.Status status = emptyViewStatusFromError(emptyCard.throwable().or(new EmptyThrowable()));
        ((EmptyView) view).setStatus(status);
    }
}
