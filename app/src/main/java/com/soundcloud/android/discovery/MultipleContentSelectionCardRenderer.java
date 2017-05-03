package com.soundcloud.android.discovery;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.discovery.DiscoveryCard.MultipleContentSelectionCard;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.os.Parcelable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MultipleContentSelectionCardRenderer implements CellRenderer<MultipleContentSelectionCard> {

    private final Map<Urn, Parcelable> scrollingState = new HashMap<>();
    private final SelectionItemAdapterFactory selectionItemAdapterFactory;

    @Inject
    MultipleContentSelectionCardRenderer(SelectionItemAdapterFactory selectionItemAdapterFactory) {
        this.selectionItemAdapterFactory = selectionItemAdapterFactory;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        View view = LayoutInflater.from(viewGroup.getContext())
                                     .inflate(R.layout.multiple_content_selection_card, viewGroup, false);
        initCarousel(view, ButterKnife.findById(view, R.id.selection_playlists_carousel));
        return view;
    }

    private void initCarousel(View cardView, RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();
        final SelectionItemAdapter adapter = selectionItemAdapterFactory.create();

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        cardView.setTag(adapter);
    }

    @Override
    public void bindItemView(int position, View view, List<MultipleContentSelectionCard> list) {
        MultipleContentSelectionCard selectionCard = list.get(position);
        bindTitle(view, selectionCard);
        bindDescription(view, selectionCard);
        bindCarousel(view, selectionCard);
    }

    private void bindTitle(View view, MultipleContentSelectionCard selectionCard) {
        TextView title = ButterKnife.findById(view, R.id.selection_title);
        if (selectionCard.title().isPresent()) {
            title.setVisibility(View.VISIBLE);
            title.setText(selectionCard.title().get());
        } else {
            title.setVisibility(View.GONE);
        }
    }

    private void bindDescription(View view, MultipleContentSelectionCard selectionCard) {
        TextView description = ButterKnife.findById(view, R.id.selection_description);
        if (selectionCard.description().isPresent()) {
            description.setVisibility(View.VISIBLE);
            description.setText(selectionCard.description().get());
        } else {
            description.setVisibility(View.GONE);
        }
    }

    private void bindCarousel(View view, MultipleContentSelectionCard selection) {
        final RecyclerView recyclerView = ButterKnife.findById(view, R.id.selection_playlists_carousel);
        final SelectionItemAdapter adapter = (SelectionItemAdapter) view.getTag();

        saveOldScrollingState(adapter, recyclerView);
        adapter.updateSelection(selection);
        loadScrollingState(adapter, recyclerView);
    }

    private void saveOldScrollingState(SelectionItemAdapter adapter, RecyclerView recyclerView) {
        adapter.selectionUrn().ifPresent(urn -> scrollingState.put(urn, recyclerView.getLayoutManager().onSaveInstanceState()));
    }

    private void loadScrollingState(SelectionItemAdapter adapter, RecyclerView recyclerView) {
        if (adapter.selectionUrn().isPresent() && scrollingState.containsKey(adapter.selectionUrn().get())) {
            recyclerView.getLayoutManager().onRestoreInstanceState(scrollingState.get(adapter.selectionUrn().get()));
        } else {
            recyclerView.scrollToPosition(0);
        }
    }
}
