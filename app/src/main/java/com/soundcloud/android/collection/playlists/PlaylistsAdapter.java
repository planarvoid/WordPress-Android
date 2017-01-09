package com.soundcloud.android.collection.playlists;

import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.view.View;

import javax.inject.Inject;

public class PlaylistsAdapter extends PagingRecyclerItemAdapter<PlaylistCollectionItem, RecyclerItemAdapter.ViewHolder>
        implements PlaylistHeaderRenderer.OnSettingsClickListener, PlaylistRemoveFilterRenderer.OnRemoveFilterListener {

    private Listener listener;

    public interface Listener {
        void onPlaylistSettingsClicked(View view);

        void onRemoveFilterClicked();
    }

    @Inject
    public PlaylistsAdapter(PlaylistHeaderRenderer headerRenderer,
                            PlaylistRemoveFilterRenderer removeFilterRenderer,
                            EmptyPlaylistsRenderer emptyPlaylistsRenderer,
                            PlaylistCollectionItemRenderer playlistRenderer) {
        super(new CellRendererBinding<>(PlaylistCollectionItem.TYPE_HEADER, headerRenderer),
              new CellRendererBinding<>(PlaylistCollectionItem.TYPE_REMOVE_FILTER, removeFilterRenderer),
              new CellRendererBinding<>(PlaylistCollectionItem.TYPE_EMPTY, emptyPlaylistsRenderer),
              new CellRendererBinding<>(PlaylistCollectionItem.TYPE_PLAYLIST, playlistRenderer));

        headerRenderer.setOnSettingsClickListener(this);
        removeFilterRenderer.setOnRemoveFilterClickListener(this);
    }

    @Override
    public void onSettingsClicked(View view) {
        if (listener != null) {
            listener.onPlaylistSettingsClicked(view);
        }
    }

    @Override
    public void onRemoveFilter() {
        if (listener != null) {
            listener.onRemoveFilterClicked();
        }
    }

    void setItem(int position, PlaylistCollectionItem item) {
        getItems().set(position, item);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getUrn().getNumericId();
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getType();
    }
}
