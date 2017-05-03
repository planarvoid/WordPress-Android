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
    DiscoveryAdapter(SearchItemRenderer searchItemRenderer, MultipleContentSelectionCardRenderer multipleContentSelectionCardRenderer) {
        super(new CellRendererBinding(DiscoveryCard.Kind.SEARCH_ITEM.ordinal(), searchItemRenderer),
              new CellRendererBinding(DiscoveryCard.Kind.MULTIPLE_CONTENT_SELECTION_CARD.ordinal(), multipleContentSelectionCardRenderer));
        this.searchItemRenderer = searchItemRenderer;
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int position) {
        if (isRendererImplemented(getItem(position).kind())) {
            return getItem(position).kind().ordinal();
        }
        //TODO remove this once all the renderers are implemented
        return DiscoveryCard.Kind.SEARCH_ITEM.ordinal();
    }

    private boolean isRendererImplemented(DiscoveryCard.Kind kind) {
        //Add your item once you write a renderer
        return kind == DiscoveryCard.Kind.SEARCH_ITEM || kind == DiscoveryCard.Kind.MULTIPLE_CONTENT_SELECTION_CARD;
    }

    void setSearchListener(SearchListener searchListener) {
        searchItemRenderer.setSearchListener(searchListener);
    }
}
