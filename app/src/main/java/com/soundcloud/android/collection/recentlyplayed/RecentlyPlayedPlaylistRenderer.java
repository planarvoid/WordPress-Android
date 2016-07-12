package com.soundcloud.android.collection.recentlyplayed;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.collection.CollectionItem;
import com.soundcloud.android.collection.RecentlyPlayedCollectionItem;
import com.soundcloud.android.collection.RecentlyPlayedItem;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

@AutoFactory(allowSubclasses = true)
public class RecentlyPlayedPlaylistRenderer implements CellRenderer<CollectionItem> {

    private final ImageOperations imageOperations;
    private final Resources resources;
    private final Navigator navigator;
    private final boolean fixedWidth;

    public RecentlyPlayedPlaylistRenderer(boolean fixedWidth,
                                          @Provided ImageOperations imageOperations,
                                          @Provided Resources resources,
                                          @Provided Navigator navigator) {
        this.fixedWidth = fixedWidth;
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        int layout = fixedWidth
                     ? R.layout.collection_recently_played_playlist_item_fixed_width
                     : R.layout.collection_recently_played_playlist_item;

        return LayoutInflater.from(parent.getContext())
                             .inflate(layout, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<CollectionItem> list) {
        final RecentlyPlayedCollectionItem item = (RecentlyPlayedCollectionItem) list.get(position);
        final RecentlyPlayedItem playlist = item.getRecentlyPlayedItem();

        setImage(view, playlist);
        setTitle(view, playlist.getTitle());
        setTrackCount(view, playlist);
        setType(view, playlist.isAlbum()
                      ? R.string.collections_recently_played_album
                      : R.string.collections_recently_played_playlist);

        view.setOnClickListener(goToPlaylist(playlist));
    }

    private void setTitle(View view, String title) {
        ButterKnife.<TextView>findById(view, R.id.title).setText(title);
    }

    private void setType(View view, int resId) {
        ButterKnife.<TextView>findById(view, R.id.recently_played_type).setText(resId);
    }

    private void setImage(View view, ImageResource imageResource) {
        final ImageView artwork = (ImageView) view.findViewById(R.id.artwork);
        imageOperations.displayInAdapterView(imageResource, getImageSize(), artwork);
    }

    private void setTrackCount(View view, RecentlyPlayedItem playlist) {
        final TextView trackCount = (TextView) view.findViewById(R.id.track_count);
        trackCount.setText(String.valueOf(playlist.getTrackCount()));
    }

    private ApiImageSize getImageSize() {
        return ApiImageSize.getFullImageSize(resources);
    }

    private View.OnClickListener goToPlaylist(final RecentlyPlayedItem playlist) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigator.openPlaylist(view.getContext(), playlist.getUrn(), Screen.COLLECTIONS);
            }
        };
    }
}
