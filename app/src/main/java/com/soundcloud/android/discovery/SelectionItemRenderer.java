package com.soundcloud.android.discovery;


import static butterknife.ButterKnife.findById;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.StyledImageView;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.IdRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class SelectionItemRenderer implements CellRenderer<SelectionItem> {

    private final ImageOperations imageOperations;
    private final Navigator navigator;

    @Inject
    SelectionItemRenderer(ImageOperations imageOperations, Navigator navigator) {
        this.imageOperations = imageOperations;
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                             .inflate(R.layout.carousel_playlist_item_fixed_width, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<SelectionItem> list) {
        final SelectionItem selectionItem = list.get(position);

        bindImage(view, selectionItem);
        bindTitle(view, selectionItem.shortTitle());
        bindSubtitle(view, selectionItem.shortSubtitle());
        bindCount(view, selectionItem.count());
        bindOverflowMenu(view);
        bindClickHandling(view, selectionItem);
    }

    private void bindOverflowMenu(View view) {
        // not MVP, since there are no additional actions to take after opening the overflow menu
        findById(view, R.id.overflow_button).setVisibility(View.GONE);
    }

    private void bindSubtitle(View view, Optional<String> subtitle) {
        bindText(view, R.id.secondary_text, subtitle);
    }

    private void bindCount(View view, Optional<Integer> count) {
        bindIntegerText(view, R.id.track_count, count);
    }

    private void bindImage(View view, SelectionItem selectionItem) {
        final StyledImageView styledImageView = findById(view, R.id.artwork);
        styledImageView.showWithPlaceholder(selectionItem.artworkUrlTemplate(), selectionItem.artworkStyle(), String.valueOf(selectionItem.hashCode()), imageOperations);
    }

    private void bindTitle(View view, Optional<String> title) {
        bindText(view, R.id.title, title);
    }

    private void bindText(View view, @IdRes int id, Optional<String> text) {
        final TextView textView = findById(view, id);
        if (text.isPresent()) {
            textView.setText(text.get());
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    private void bindIntegerText(View view, @IdRes int id, Optional<Integer> integer) {
        final TextView textView = findById(view, id);
        if (integer.isPresent()) {
            textView.setText(String.valueOf(integer.get()));
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    private void bindClickHandling(View view, final SelectionItem selectionItem) {
        view.setOnClickListener(selectionItem.onClickListener(navigator));
    }
}
