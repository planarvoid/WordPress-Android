package com.soundcloud.android.discovery;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.discovery.DiscoveryCard.MultipleContentSelectionCard;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.IdRes;
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
        bindText(view, R.id.selection_title, selectionCard.title());
    }

    private void bindDescription(View view, MultipleContentSelectionCard selectionCard) {
        bindText(view, R.id.selection_description, selectionCard.description());
    }

    private void bindText(View view, @IdRes int id, Optional<String> text) {
        TextView textView = ButterKnife.findById(view, id);
        if (text.isPresent()) {
            textView.setVisibility(View.VISIBLE);
            textView.setText(text.get());
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    private void bindCarousel(View view, MultipleContentSelectionCard selectionCard) {
        final RecyclerView recyclerView = ButterKnife.findById(view, R.id.selection_playlists_carousel);
        final SelectionItemAdapter adapter = (SelectionItemAdapter) view.getTag();

        saveOldScrollingState(adapter, recyclerView);
        adapter.updateSelection(selectionCard);
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
