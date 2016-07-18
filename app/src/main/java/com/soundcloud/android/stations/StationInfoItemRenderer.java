package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationTypes.getHumanReadableType;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class StationInfoItemRenderer implements CellRenderer<StationInfo> {

    private final ImageOperations imageOperations;
    private final Resources resources;

    @Inject
    StationInfoItemRenderer(Resources resources, ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.station_info_view, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<StationInfo> items) {
        StationInfo info = items.get(position);

        bindArtwork(info, itemView);
        bindTextViews(info, itemView);
        bindPlayButton(itemView);
    }

    private void bindPlayButton(View itemView) {
        // TODO: Add Station Play Click listener
        ButterKnife.findById(itemView, R.id.btn_play).setVisibility(View.VISIBLE);
    }

    private void bindTextViews(StationInfo info, View itemView) {
        ButterKnife.<TextView>findById(itemView, R.id.station_type)
                .setText(getHumanReadableType(resources, info.getType()));
        ButterKnife.<TextView>findById(itemView, R.id.title).setText(info.getTitle());
    }

    private void bindArtwork(StationInfo info, View headerView) {
        final ApiImageSize artworkSize = ApiImageSize.getFullImageSize(headerView.getResources());
        ImageView artworkView = ButterKnife.findById(headerView, R.id.artwork);
        imageOperations.displayWithPlaceholder(info, artworkSize, artworkView);
    }
}
