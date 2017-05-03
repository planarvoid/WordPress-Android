package com.soundcloud.android.discovery;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.discovery.DiscoveryCard.MultipleContentSelectionCard;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.java.optional.Optional;

import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory
class SelectionItemAdapter extends RecyclerItemAdapter<SelectionItem, RecyclerView.ViewHolder> {

    private Optional<Urn> selectionUrn = Optional.absent();

    SelectionItemAdapter(@Provided SelectionItemRenderer selectionItemRenderer) {
        super(selectionItemRenderer);
    }

    public Optional<Urn> selectionUrn() {
        return selectionUrn;
    }

    void updateSelection(MultipleContentSelectionCard selection) {
        selectionUrn = Optional.of(selection.selectionUrn());
        clear();
        onNext(selection.selectionItems());
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return 0;
    }
}
