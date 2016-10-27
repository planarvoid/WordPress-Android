package com.soundcloud.android.discovery;

import static com.soundcloud.android.utils.ErrorUtils.emptyViewStatusFromError;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.EmptyViewBuilder;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class EmptyDiscoveryItemRenderer implements CellRenderer<DiscoveryItem> {

    @Inject
    public EmptyDiscoveryItemRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        Context context = parent.getContext();
        EmptyView emptyView = new EmptyViewBuilder()
                .withMessageText(context.getString(R.string.discovery_empty))
                .build(context);
        emptyView.setPadding(0, ViewUtils.dpToPx(context, 78), 0, 0);
        return emptyView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<DiscoveryItem> items) {
        final EmptyViewItem emptyViewItem = (EmptyViewItem) items.get(position);

        final EmptyView.Status status = emptyViewStatusFromError(emptyViewItem.getThrowable());
        ((EmptyView) itemView).setStatus(status);
    }
}
