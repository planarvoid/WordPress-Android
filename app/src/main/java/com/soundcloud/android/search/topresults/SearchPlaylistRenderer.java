package com.soundcloud.android.search.topresults;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.java.optional.Optional;
import io.reactivex.subjects.PublishSubject;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

@AutoFactory
class SearchPlaylistRenderer implements CellRenderer<SearchItem.Playlist> {
    private final PlaylistItemRenderer playlistItemRenderer;
    private final PublishSubject<SearchItem.Playlist> playlistClick;

    SearchPlaylistRenderer(@Provided PlaylistItemRenderer playlistItemRenderer, PublishSubject<SearchItem.Playlist> playlistClick) {
        this.playlistItemRenderer = playlistItemRenderer;
        this.playlistClick = playlistClick;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return playlistItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchItem.Playlist> items) {
        final SearchItem.Playlist playlist = items.get(position);
        itemView.setOnClickListener(view -> playlistClick.onNext(playlist));
        playlistItemRenderer.bindPlaylistView(playlist.playlistItem(), itemView, Optional.absent(), Optional.of(playlist.source().key));
    }

}
