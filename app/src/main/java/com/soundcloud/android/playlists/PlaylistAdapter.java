package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.NowPlayingAdapter;

import android.view.View;

@AutoFactory(allowSubclasses = true)
public class PlaylistAdapter extends RecyclerItemAdapter<ListItem, RecyclerItemAdapter.ViewHolder>
        implements NowPlayingAdapter {

    private static final int PLAYLIST_INFO_ITEM_TYPE = 0;
    private static final int TRACK_ITEM_TYPE = 1;

    public PlaylistAdapter(PlaylistHeaderPresenter headerPresenter,
                           @Provided PlaylistTrackItemRenderer trackItemRenderer) {
        super(new CellRendererBinding<>(PLAYLIST_INFO_ITEM_TYPE, new PlaylistHeaderRenderer(headerPresenter)),
                new CellRendererBinding<>(TRACK_ITEM_TYPE, trackItemRenderer));
    }

    @Override
    public int getBasicItemViewType(int position) {
        if (getItem(position) instanceof PlaylistHeaderItem) {
            return PLAYLIST_INFO_ITEM_TYPE;
        } else {
            return TRACK_ITEM_TYPE;
        }
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (ListItem item : getItems()) {
            if (item instanceof TrackItem) {
                ((TrackItem) item).setIsPlaying(item.getUrn().equals(currentlyPlayingUrn));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

}
