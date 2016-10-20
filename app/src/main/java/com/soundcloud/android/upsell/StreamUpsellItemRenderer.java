package com.soundcloud.android.upsell;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stream.StreamItem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class StreamUpsellItemRenderer implements CellRenderer<StreamItem> {

    private final UpsellItemRenderer upsellItemRenderer;

    @Inject
    public StreamUpsellItemRenderer(UpsellItemRenderer upsellItemRenderer) {
        this.upsellItemRenderer = upsellItemRenderer;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        upsellItemRenderer.createItemView(parent);
        return LayoutInflater.from(parent.getContext())
                .inflate(R.layout.stream_upsell_card, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<StreamItem> items) {
        upsellItemRenderer.bindItemView(position, itemView);
    }

    public void setListener(UpsellItemRenderer.Listener listener) {
        upsellItemRenderer.setListener(listener);
    }
}
