package com.soundcloud.android.profile;

import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class UserSoundsAdapter extends RecyclerItemAdapter<UserSoundsItem, UserSoundsAdapter.ViewHolder> {
    static final int TYPE_DIVIDER = 0;
    static final int TYPE_HEADER = 1;
    static final int TYPE_VIEW_ALL = 2;
    static final int TYPE_TRACK_CARD = 3;
    static final int TYPE_TRACK_ITEM = 4;
    static final int TYPE_PLAYLIST_CARD = 5;
    static final int TYPE_PLAYLIST_ITEM = 6;

    @Inject
    UserSoundsAdapter(DividerRenderer dividerRenderer,
                      HeaderRenderer headerRenderer,
                      ViewAllRenderer viewAllRenderer,
                      UserSoundsTrackCardRenderer trackCardRenderer,
                      UserSoundsTrackItemRenderer trackItemRenderer,
                      UserSoundsPlaylistCardRenderer playlistCardRenderer,
                      UserSoundsPlaylistItemRenderer playlistItemRenderer) {
        super(new CellRendererBinding<>(TYPE_DIVIDER, dividerRenderer),
                new CellRendererBinding<>(TYPE_HEADER, headerRenderer),
                new CellRendererBinding<>(TYPE_VIEW_ALL, viewAllRenderer),
                new CellRendererBinding<>(TYPE_TRACK_CARD, trackCardRenderer),
                new CellRendererBinding<>(TYPE_TRACK_ITEM, trackItemRenderer),
                new CellRendererBinding<>(TYPE_PLAYLIST_CARD, playlistCardRenderer),
                new CellRendererBinding<>(TYPE_PLAYLIST_ITEM, playlistItemRenderer));
    }

    @Override
    public int getBasicItemViewType(int position) {
        final UserSoundsItem item = getItem(position);

        switch (item.getItemType()) {
            case UserSoundsItem.TYPE_DIVIDER:
                return TYPE_DIVIDER;
            case UserSoundsItem.TYPE_HEADER:
                return TYPE_HEADER;
            case UserSoundsItem.TYPE_VIEW_ALL:
                return TYPE_VIEW_ALL;
            case UserSoundsItem.TYPE_TRACK:
                if (item.getCollectionType() == UserSoundsTypes.SPOTLIGHT) {
                    return TYPE_TRACK_CARD;
                } else {
                    return TYPE_TRACK_ITEM;
                }
            case UserSoundsItem.TYPE_PLAYLIST:
                if (item.getCollectionType() == UserSoundsTypes.SPOTLIGHT) {
                    return TYPE_PLAYLIST_CARD;
                } else {
                    return TYPE_PLAYLIST_ITEM;
                }
            default:
                throw new IllegalArgumentException("No User Sound Item of the given type");
        }
    }

    @Override
    protected UserSoundsAdapter.ViewHolder createViewHolder(View itemView) {
        return new UserSoundsAdapter.ViewHolder(itemView);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
