package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

@AutoFactory
public class NewPlaylistDetailsHeaderRenderer implements CellRenderer<PlaylistDetailsHeaderItem> {

    private final PlaylistCoverRenderer playlistCoverRenderer;
    private final PlaylistEngagementsRenderer playlistEngagementsRenderer;
    private final PlaylistDetailsInputs playlistDetailsInputs;

    public NewPlaylistDetailsHeaderRenderer(@Provided PlaylistCoverRenderer playlistCoverRenderer,
                                            @Provided PlaylistEngagementsRenderer playlistEngagementsRenderer,
                                            PlaylistDetailsInputs playlistDetailsInputs) {
        this.playlistCoverRenderer = playlistCoverRenderer;
        this.playlistEngagementsRenderer = playlistEngagementsRenderer;
        this.playlistDetailsInputs = playlistDetailsInputs;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.new_playlist_detail_header, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistDetailsHeaderItem> items) {

        PlaylistDetailsHeaderItem headerItem = items.get(position);
        if (headerItem.getMetadataOptional().isPresent()) {
            PlaylistDetailsMetadata metadata = headerItem.getMetadataOptional().get();
            playlistCoverRenderer.bind(itemView, metadata, playlistDetailsInputs::onHeaderPlayButtonClicked, playlistDetailsInputs::onCreatorClicked);
            playlistEngagementsRenderer.bind(itemView, playlistDetailsInputs, metadata);
        }
        // if it is not present, we just show whatever is there, which is the blank state, or the old state before a refresh
    }
}
