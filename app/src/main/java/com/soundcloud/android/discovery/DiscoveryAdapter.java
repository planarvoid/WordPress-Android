package com.soundcloud.android.discovery;


import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.search.SearchItemRenderer;
import com.soundcloud.android.search.SearchItemRenderer.SearchListener;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class DiscoveryAdapter extends RecyclerItemAdapter<DiscoveryCard, RecyclerView.ViewHolder> {
    private final SearchItemRenderer searchItemRenderer;

    @SuppressWarnings("unchecked")
    @Inject
    DiscoveryAdapter(SearchItemRenderer searchItemRenderer) {
        super(new CellRendererBinding(DiscoveryCard.Kind.SEARCH_ITEM.ordinal(), searchItemRenderer));
        this.searchItemRenderer = searchItemRenderer;
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).kind().ordinal();
    }

    void setSearchListener(SearchListener searchListener) {
        searchItemRenderer.setSearchListener(searchListener);
    }
}
