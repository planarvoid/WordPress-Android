package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationTypes.getHumanReadableType;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stations.StationInfoAdapter.StationInfoClickListener;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

@AutoFactory
class StationInfoItemRenderer implements CellRenderer<StationInfo> {

    private final StationInfoClickListener clickListener;
    private final ImageOperations imageOperations;
    private final Resources resources;

    StationInfoItemRenderer(StationInfoClickListener listener,
                            @Provided Resources resources,
                            @Provided ImageOperations imageOperations) {
        this.clickListener = listener;
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
        final View playButton = ButterKnife.findById(itemView, R.id.btn_play);
        playButton.setVisibility(View.VISIBLE);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onPlayButtonClicked(view.getContext());
            }
        });
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
