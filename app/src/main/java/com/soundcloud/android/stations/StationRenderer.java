package com.soundcloud.android.stations;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.res.Resources;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class StationRenderer implements CellRenderer<Station> {
    private final ImageOperations imageOperations;
    private final Resources resources;
    private final StartStationPresenter startStationPresenter;

    @Inject
    public StationRenderer(ImageOperations imageOperations,
                           Resources resources,
                           StartStationPresenter startStationPresenter) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.startStationPresenter = startStationPresenter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.station_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<Station> stations) {
        final Station station = stations.get(position);
        final ImageView artwork = ButterKnife.findById(view, R.id.artwork);
        final TextView title = ButterKnife.findById(view, R.id.title);
        final TextView type = ButterKnife.findById(view, R.id.type);

        view.setOnClickListener(startStation(station));
        ((CardView) view).setPreventCornerOverlap(false);
        title.setText(station.getTitle());
        type.setText(getHumanReadableType(station.getType()));

        imageOperations.displayInAdapterView(
                station.getUrn(),
                ApiImageSize.getFullImageSize(resources),
                artwork
        );
    }

    private String getHumanReadableType(String type) {
        switch (type) {
            case StationTypes.TRACK:
                return resources.getString(R.string.station_type_track);
            case StationTypes.GENRE:
                return resources.getString(R.string.station_type_genre);
            case StationTypes.CURATOR:
                return resources.getString(R.string.station_type_curator);
            default:
                throw new IllegalArgumentException("Unknown station type: " + type);
        }
    }

    private View.OnClickListener startStation(final Station station) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startStationPresenter.startStation(view.getContext(), station.getUrn());
            }
        };
    }
}
