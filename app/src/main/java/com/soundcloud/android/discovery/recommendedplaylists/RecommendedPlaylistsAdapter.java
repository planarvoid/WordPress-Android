package com.soundcloud.android.discovery.recommendedplaylists;

import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.java.optional.Optional;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class RecommendedPlaylistsAdapter extends RecyclerItemAdapter<PlaylistItem, RecyclerView.ViewHolder>{

    private static final int RECOMMENDED_PLAYLIST_TYPE = 0;
    private Optional<String> key = Optional.absent();

    @Inject
    RecommendedPlaylistsAdapter(RecommendedPlaylistItemRenderer renderer) {
        super(renderer);
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return RECOMMENDED_PLAYLIST_TYPE;
    }

    boolean hasBucketItem() {
        return key.isPresent();
    }

    String bucketId() {
        return key.get();
    }

    void setRecommendedTracksBucketItem(RecommendedPlaylistsBucketItem recommendedPlaylists) {
        key = Optional.of(recommendedPlaylists.key());
        clear();
        onNext(recommendedPlaylists.playlists());
    }
}
