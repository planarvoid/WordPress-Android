package com.soundcloud.android.discovery;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.search.SearchItemRenderer;
import com.soundcloud.android.search.SearchItemRenderer.SearchListener;
import io.reactivex.Observable;

import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class DiscoveryAdapter extends RecyclerItemAdapter<DiscoveryCard, RecyclerView.ViewHolder> {

    private final SingleSelectionContentCardRenderer singleSelectionContentCardRenderer;
    private final MultipleContentSelectionCardRenderer multipleContentSelectionCardRenderer;

    @SuppressWarnings("unchecked")
    DiscoveryAdapter(@Provided SearchItemRenderer searchItemRenderer,
                     @Provided SingleSelectionContentCardRenderer singleSelectionContentCardRenderer,
                     @Provided MultipleContentSelectionCardRenderer multipleContentSelectionCardRenderer,
                     @Provided EmptyCardRenderer emptyCardRenderer,
                     SearchListener searchListener) {
        super(new CellRendererBinding(DiscoveryCard.Kind.SEARCH_ITEM.ordinal(), searchItemRenderer),
              new CellRendererBinding(DiscoveryCard.Kind.SINGLE_CONTENT_SELECTION_CARD.ordinal(), singleSelectionContentCardRenderer),
              new CellRendererBinding(DiscoveryCard.Kind.MULTIPLE_CONTENT_SELECTION_CARD.ordinal(), multipleContentSelectionCardRenderer),
              new CellRendererBinding(DiscoveryCard.Kind.EMPTY_CARD.ordinal(), emptyCardRenderer));
        searchItemRenderer.setSearchListener(searchListener);
        this.singleSelectionContentCardRenderer = singleSelectionContentCardRenderer;
        this.multipleContentSelectionCardRenderer = multipleContentSelectionCardRenderer;
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).kind().ordinal();
    }

    Observable<SelectionItem> selectionItemClick() {
        return Observable.merge(singleSelectionContentCardRenderer.selectionItemClick(), multipleContentSelectionCardRenderer.selectionItemClick());
    }
}
