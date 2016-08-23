package com.soundcloud.android.collection.recentlyplayed;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stations.StartStationHandler;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class RecentlyPlayedStationRenderer implements CellRenderer<RecentlyPlayedPlayableItem> {

    private final boolean fixedWidth;
    private final ImageOperations imageOperations;
    private final Resources resources;
    private final StartStationHandler stationHandler;

    RecentlyPlayedStationRenderer(boolean fixedWidth,
                                  @Provided ImageOperations imageOperations,
                                  @Provided Resources resources,
                                  @Provided StartStationHandler stationHandler) {
        this.fixedWidth = fixedWidth;
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.stationHandler = stationHandler;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        int layout = fixedWidth
                     ? R.layout.collection_recently_played_station_item_fixed_width
                     : R.layout.collection_recently_played_station_item;

        return LayoutInflater.from(parent.getContext())
                             .inflate(layout, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<RecentlyPlayedPlayableItem> list) {
        final RecentlyPlayedPlayableItem station = list.get(position);

        setImage(view, station);
        setTitle(view, station.getTitle());
        setType(view, getStationType(station));
        view.setOnClickListener(goToStation(station));
    }

    private void setTitle(View view, String title) {
        ButterKnife.<TextView>findById(view, R.id.title).setText(title);
    }

    private int getStationType(RecentlyPlayedPlayableItem station) {
        Urn urn = station.getUrn();

        if (urn.isArtistStation()) {
            return R.string.collections_recently_played_artist_station;
        } else if (urn.isTrackStation()) {
            return R.string.collections_recently_played_track_station;
        } else {
            return R.string.collections_recently_played_other_station;
        }
    }

    private void setType(View view, int resId) {
        ButterKnife.<TextView>findById(view, R.id.recently_played_type).setText(resId);
    }

    private void setImage(View view, ImageResource imageResource) {
        final ImageView artwork = (ImageView) view.findViewById(R.id.artwork);
        imageOperations.displayInAdapterView(imageResource, getImageSize(), artwork);
    }

    private ApiImageSize getImageSize() {
        return ApiImageSize.getFullImageSize(resources);
    }

    private View.OnClickListener goToStation(final RecentlyPlayedPlayableItem station) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stationHandler.startStation(view.getContext(), station.getUrn());
            }
        };
    }

}
