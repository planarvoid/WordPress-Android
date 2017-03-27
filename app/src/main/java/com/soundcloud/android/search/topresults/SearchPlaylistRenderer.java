package com.soundcloud.android.search.topresults;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.java.optional.Optional;
import rx.subjects.PublishSubject;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

@AutoFactory
class SearchPlaylistRenderer implements CellRenderer<SearchItem.Playlist> {
    private final PlaylistItemRenderer playlistItemRenderer;
    private final PublishSubject<SearchItem> searchItemClicked;

    @Inject
    SearchPlaylistRenderer(@Provided PlaylistItemRenderer playlistItemRenderer, PublishSubject<SearchItem> searchItemClicked) {
        this.playlistItemRenderer = playlistItemRenderer;
        this.searchItemClicked = searchItemClicked;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return playlistItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchItem.Playlist> items) {
        final SearchItem.Playlist playlist = items.get(position);
        itemView.setOnClickListener(view -> searchItemClicked.onNext(playlist));
        playlistItemRenderer.bindPlaylistView(playlist.playlistItem(), itemView, Optional.absent(), Optional.of(playlist.source().key));
    }

}
