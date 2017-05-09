package com.soundcloud.android.discovery.systemplaylist;

import android.view.View;
import android.view.ViewGroup;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.olddiscovery.recommendations.QuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class SystemPlaylistTrackRenderer implements CellRenderer<SystemPlaylistItem.Track> {
    private final TrackItemRenderer trackItemRenderer;

    SystemPlaylistTrackRenderer(TrackItemRenderer.Listener listener,
                                @Provided TrackItemRenderer trackItemRenderer) {
        this.trackItemRenderer = trackItemRenderer;

        this.trackItemRenderer.setListener(listener);
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SystemPlaylistItem.Track> items) {
        SystemPlaylistItem.Track trackItem = items.get(position);

        final TrackSourceInfo info;
        if (trackItem.isNewForYou()) {
            info = new TrackSourceInfo(Screen.NEW_FOR_YOU.get(), true);
            info.setSource(DiscoverySource.NEW_FOR_YOU.value(), Strings.EMPTY);
            trackItem.queryUrn().ifPresent(urn -> info.setQuerySourceInfo(QuerySourceInfo.create(position - SystemPlaylistPresenter.NUM_EXTRA_ITEMS, urn)));
        } else {
            // TODO (REC-1174): construct tracking info for system playlists track clicks here
            info = null;
        }

        trackItemRenderer.bindSystemPlaylistTrackView(items.get(position).track(),
                                                      itemView,
                                                      position,
                                                      Optional.fromNullable(info));
    }
}
