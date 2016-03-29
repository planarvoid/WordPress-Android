package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.search.PlaylistTagsPresenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class PlaylistTagRenderer implements CellRenderer<PlaylistDiscoveryItem> {

    private final PlaylistTagsPresenter playlistTagsPresenter;

    @Inject
    PlaylistTagRenderer(PlaylistTagsPresenter playlistTagsPresenter) {
        this.playlistTagsPresenter = playlistTagsPresenter;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        final View tagsView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.playlist_tags, viewGroup, false);
        // we don't use this empty view, it is for playlist tags (remove it when we remove the feature flag)
        tagsView.findViewById(android.R.id.empty).setVisibility(View.GONE);
        return tagsView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistDiscoveryItem> discoveryItems) {
        final List<String> recentTags = discoveryItems.get(position).getRecentTags();
        final List<String> popularTags = discoveryItems.get(position).getPopularTags();
        if (!recentTags.isEmpty()) {
            playlistTagsPresenter.displayRecentTags(itemView, recentTags);
        }
        if (!popularTags.isEmpty()) {
            playlistTagsPresenter.displayPopularTags(itemView, popularTags);
        }
    }

    void setOnTagClickListener(PlaylistTagsPresenter.Listener itemListener) {
        playlistTagsPresenter.setListener(itemListener);
    }
}
