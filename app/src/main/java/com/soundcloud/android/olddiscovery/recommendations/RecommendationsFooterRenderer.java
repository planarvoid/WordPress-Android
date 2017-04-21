package com.soundcloud.android.olddiscovery.recommendations;

import com.soundcloud.android.R;
import com.soundcloud.android.olddiscovery.OldDiscoveryItem;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class RecommendationsFooterRenderer implements CellRenderer<OldDiscoveryItem> {

    @Inject
    public RecommendationsFooterRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.recommendations_footer_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<OldDiscoveryItem> items) {

    }
}
