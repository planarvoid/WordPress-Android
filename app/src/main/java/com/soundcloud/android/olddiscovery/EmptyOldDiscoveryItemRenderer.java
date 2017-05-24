package com.soundcloud.android.olddiscovery;

import static com.soundcloud.android.utils.ErrorUtils.emptyViewStatusFromError;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.EmptyViewBuilder;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class EmptyOldDiscoveryItemRenderer implements CellRenderer<OldDiscoveryItem> {

    @Inject
    EmptyOldDiscoveryItemRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        Context context = parent.getContext();
        return new EmptyViewBuilder()
                .withMessageText(context.getString(R.string.discovery_empty))
                .withPadding(R.dimen.empty_card_left_padding,
                             R.dimen.empty_card_top_padding,
                             R.dimen.empty_card_right_padding,
                             R.dimen.empty_card_bottom_padding)
                .build(context);
    }

    @Override
    public void bindItemView(int position, View itemView, List<OldDiscoveryItem> items) {
        final EmptyViewItem emptyViewItem = (EmptyViewItem) items.get(position);

        final EmptyView.Status status = emptyViewStatusFromError(emptyViewItem.getThrowable());
        ((EmptyView) itemView).setStatus(status);
    }
}
