package com.soundcloud.android.stations;

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

class StationRenderer implements CellRenderer<Station> {
    private final ImageOperations imageOperations;
    private final Resources resources;

    @Inject
    public StationRenderer(ImageOperations imageOperations, Resources resources) {
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.station_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<Station> stations) {
        final Station station = stations.get(position);
        final ImageView artwork = (ImageView) view.findViewById(R.id.artwork);
        final TextView title = (TextView) view.findViewById(R.id.title);

        title.setText(station.getTitle());

        imageOperations.displayInAdapterView(
                station.getUrn(),
                ApiImageSize.getFullImageSize(resources),
                artwork
        );
    }

}
