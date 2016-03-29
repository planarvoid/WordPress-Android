package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.checks.Preconditions;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class TrackSuggestionItemRenderer implements CellRenderer<TrackSuggestionItem> {

    interface OnTrackClickListener {
        void onTrackClicked(String searchQuery);
    }

    private OnTrackClickListener onTrackClickListener;

    @Inject
    TrackSuggestionItemRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return null;
    }

    @Override
    public void bindItemView(int position, View itemView, List<TrackSuggestionItem> items) {

    }

    void setOnTrackClickListener(OnTrackClickListener listener) {
        Preconditions.checkArgument(listener != null, "Click listener must not be null");
        this.onTrackClickListener = listener;
    }
}
