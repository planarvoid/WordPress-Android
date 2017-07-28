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
import java.util.Map;
import java.util.WeakHashMap;

@AutoFactory
class SearchPlaylistRenderer implements CellRenderer<SearchItem.Playlist> {

    private final PlaylistItemRenderer playlistItemRenderer;
    private final PublishSubject<UiAction.PlaylistClick> playlistClick;
    private final Map<View, UiAction.PlaylistClick> argsMap = new WeakHashMap<>();

    SearchPlaylistRenderer(@Provided PlaylistItemRenderer playlistItemRenderer, PublishSubject<UiAction.PlaylistClick> playlistClick) {
        this.playlistItemRenderer = playlistItemRenderer;
        this.playlistClick = playlistClick;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View itemView = playlistItemRenderer.createItemView(parent);
        itemView.setOnClickListener(view -> playlistClick.onNext(argsMap.get(itemView)));
        return itemView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchItem.Playlist> items) {
        final SearchItem.Playlist playlist = items.get(position);
        argsMap.put(itemView, playlist.clickAction());
        playlistItemRenderer.bindPlaylistView(playlist.playlistItem(), itemView, Optional.absent(), Optional.of(playlist.clickAction().clickParams().get().clickSource().key));
    }

}
