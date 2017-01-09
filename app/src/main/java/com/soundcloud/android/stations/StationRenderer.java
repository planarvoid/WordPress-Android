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

class StationRenderer implements CellRenderer<StationViewModel> {
    private final ImageOperations imageOperations;
    private final Resources resources;
    private final StartStationHandler stationHandler;

    @Inject
    public StationRenderer(ImageOperations imageOperations,
                           Resources resources,
                           StartStationHandler stationHandler) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.stationHandler = stationHandler;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.station_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<StationViewModel> stations) {
        final StationViewModel stationViewModel = stations.get(position);
        final StationRecord station = stationViewModel.getStation();
        final ImageView artwork = ButterKnife.findById(view, R.id.artwork);
        final TextView title = ButterKnife.findById(view, R.id.title);
        final TextView type = ButterKnife.findById(view, R.id.type);
        final TextView nowPlaying = ButterKnife.findById(view, R.id.now_playing);

        view.setOnClickListener(startStation(station));
        title.setText(station.getTitle());

        if (stationViewModel.isPlaying()) {
            type.setVisibility(View.GONE);
            nowPlaying.setVisibility(View.VISIBLE);
        } else {
            nowPlaying.setVisibility(View.GONE);
            type.setText(getHumanReadableType(resources, station.getType()));
            type.setVisibility(View.VISIBLE);
        }

        imageOperations.displayInAdapterView(
                station,
                ApiImageSize.getFullImageSize(resources),
                artwork
        );
    }

    private View.OnClickListener startStation(final StationRecord station) {
        return view -> stationHandler.startStation(view.getContext(), station.getUrn());
    }
}
