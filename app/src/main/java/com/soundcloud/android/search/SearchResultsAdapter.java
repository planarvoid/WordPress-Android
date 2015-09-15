package com.soundcloud.android.search;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.ViewTypes;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.FollowableUserItemRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class SearchResultsAdapter extends RecyclerItemAdapter<ListItem, SearchResultsAdapter.SearchViewHolder> {

    static final int TYPE_USER = ViewTypes.DEFAULT_VIEW_TYPE;
    static final int TYPE_TRACK = ViewTypes.DEFAULT_VIEW_TYPE + 1;
    static final int TYPE_PLAYLIST = ViewTypes.DEFAULT_VIEW_TYPE + 2;

    @Inject
    SearchResultsAdapter(FollowableUserItemRenderer userRenderer,
                         TrackItemRenderer trackRenderer,
                         PlaylistItemRenderer playlistRenderer) {
        super(new CellRendererBinding<>(TYPE_USER, userRenderer),
                new CellRendererBinding<>(TYPE_TRACK, trackRenderer),
                new CellRendererBinding<>(TYPE_PLAYLIST, playlistRenderer));
    }

    @Override
    public int getBasicItemViewType(int position) {
        final ListItem item = getItem(position);
        final Urn urn = item.getEntityUrn();
        if (urn.isUser()) {
            return TYPE_USER;
        } else if (urn.isTrack()) {
            return TYPE_TRACK;
        } else if (urn.isPlaylist()) {
            return TYPE_PLAYLIST;
        } else {
            throw new IllegalStateException("Unexpected item type in " + SearchResultsAdapter.class.getSimpleName());
        }
    }

    @Override
    protected SearchViewHolder createViewHolder(View itemView) {
        return new SearchViewHolder(itemView);
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        public SearchViewHolder(View itemView) {
            super(itemView);
        }
    }
}
