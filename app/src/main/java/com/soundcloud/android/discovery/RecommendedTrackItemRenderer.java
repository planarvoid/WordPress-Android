package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.java.checks.Preconditions;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class RecommendedTrackItemRenderer implements CellRenderer<RecommendedTrackItem> {

    public interface OnRecommendedTrackClickListener {
        void onRecommendedTrackClicked(RecommendedTrackItem recommendedTrackItem);
    }

    private final TrackItemRenderer trackItemRenderer;

    private OnRecommendedTrackClickListener onRecommendedTrackClickListener;

    @Inject
    public RecommendedTrackItemRenderer(TrackItemRenderer trackItemRenderer) {
        this.trackItemRenderer = trackItemRenderer;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return this.trackItemRenderer.createItemView(viewGroup);
    }

    @Override
    public void bindItemView(int position, View itemView, List<RecommendedTrackItem> recommendedTrackItems) {
        this.trackItemRenderer.bindItemView(position, itemView, new ArrayList<TrackItem>(recommendedTrackItems));
        setClickListeners(itemView, recommendedTrackItems.get(position));
    }

    private void setClickListeners(View itemView, final RecommendedTrackItem recommendedTrackItem) {
        itemView.findViewById(R.id.track_list_item).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final OnRecommendedTrackClickListener clickListener = RecommendedTrackItemRenderer.this.onRecommendedTrackClickListener;
                if (clickListener != null) {
                    clickListener.onRecommendedTrackClicked(recommendedTrackItem);
                }
            }
        });
    }

    void setOnRecommendedTrackClickListener(OnRecommendedTrackClickListener listener) {
        Preconditions.checkNotNull(listener, "Click listener must not be null");
        this.onRecommendedTrackClickListener = listener;
    }
}
