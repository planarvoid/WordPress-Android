package com.soundcloud.android.collection.recentlyplayed;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.collection.CollectionItem;
import com.soundcloud.android.collection.RecentlyPlayedCollectionItem;
import com.soundcloud.android.collection.RecentlyPlayedItem;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stations.StartStationPresenter;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class RecentlyPlayedStationRenderer implements CellRenderer<CollectionItem> {

    private final ImageOperations imageOperations;
    private final Resources resources;
    private final StartStationPresenter startStationPresenter;

    @Inject
    public RecentlyPlayedStationRenderer(ImageOperations imageOperations,
                                         Resources resources,
                                         StartStationPresenter startStationPresenter) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.startStationPresenter = startStationPresenter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                             .inflate(R.layout.collection_recently_played_station_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<CollectionItem> list) {
        final RecentlyPlayedCollectionItem item = (RecentlyPlayedCollectionItem) list.get(position);
        final RecentlyPlayedItem station = item.getRecentlyPlayedItem();

        setImage(view, station);
        setTitle(view, station.getTitle());
        setType(view, getStationType(station));
        view.setOnClickListener(goToStation(station));
    }

    private void setTitle(View view, String title) {
        ButterKnife.<TextView>findById(view, R.id.title).setText(title);
    }

    private int getStationType(RecentlyPlayedItem station) {
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

    private View.OnClickListener goToStation(final RecentlyPlayedItem station) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startStationPresenter.startStation(view.getContext(), station.getUrn());
            }
        };
    }

}
