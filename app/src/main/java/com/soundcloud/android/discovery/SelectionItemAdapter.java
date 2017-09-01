package com.soundcloud.android.discovery;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.discovery.DiscoveryCardViewModel.MultipleContentSelectionCard;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.java.optional.Optional;
import io.reactivex.subjects.PublishSubject;

import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class SelectionItemAdapter extends RecyclerItemAdapter<SelectionItemViewModel, RecyclerView.ViewHolder> {

    private Optional<Urn> selectionUrn = Optional.absent();

    SelectionItemAdapter(@Provided SelectionItemRendererFactory selectionItemRendererFactory, PublishSubject<SelectionItemViewModel> selectionItemClickListener) {
        super(selectionItemRendererFactory.create(selectionItemClickListener));
    }

    public Optional<Urn> selectionUrn() {
        return selectionUrn;
    }

    void updateSelection(MultipleContentSelectionCard selection) {
        selectionUrn = Optional.of(selection.getSelectionUrn());
        clear();
        onNext(selection.getSelectionItems());
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
