package com.soundcloud.android.collection.playlists;

import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.ListDiffUtilCallback;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.UpdatableRecyclerItemAdapter;
import com.soundcloud.java.objects.MoreObjects;

import android.support.v7.util.DiffUtil;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

public class PlaylistsAdapter extends UpdatableRecyclerItemAdapter<PlaylistCollectionItem, RecyclerItemAdapter.ViewHolder>
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

    @Override
    protected DiffUtil.Callback createDiffUtilCallback(List<PlaylistCollectionItem> oldList, List<PlaylistCollectionItem> newList) {
        return new ListDiffUtilCallback<PlaylistCollectionItem>(oldList, newList) {
            @Override
            protected boolean areItemsTheSame(PlaylistCollectionItem oldItem, PlaylistCollectionItem newItem) {
                if (oldItem.getType() == PlaylistCollectionItem.TYPE_PLAYLIST && newItem.getType() == PlaylistCollectionItem.TYPE_PLAYLIST) {
                    final PlaylistCollectionPlaylistItem oldPlaylist = (PlaylistCollectionPlaylistItem) oldItem;
                    final PlaylistCollectionPlaylistItem newPlaylist = (PlaylistCollectionPlaylistItem) newItem;
                    return MoreObjects.equal(oldPlaylist.getUrn(), newPlaylist.getUrn());
                } else {
                    return MoreObjects.equal(oldItem, newItem);
                }
            }
        };
    }
}
