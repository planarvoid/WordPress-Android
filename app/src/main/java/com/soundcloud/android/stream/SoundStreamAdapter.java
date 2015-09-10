package com.soundcloud.android.stream;

import com.soundcloud.android.facebookinvites.FacebookInvitesItem;
import com.soundcloud.android.facebookinvites.FacebookInvitesItemRenderer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;

import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class SoundStreamAdapter extends PagingRecyclerItemAdapter<StreamItem, SoundStreamAdapter.SoundStreamViewHolder> {

    @VisibleForTesting private static final int TRACK_ITEM_TYPE = 0;
    @VisibleForTesting private static final int PLAYLIST_ITEM_TYPE = 1;
    @VisibleForTesting private static final int FACEBOOK_INVITES_ITEM_TYPE = 2;

    private final TrackItemRenderer trackRenderer;
    private final FacebookInvitesItemRenderer facebookInvitesItemRenderer;

    @Inject
    public SoundStreamAdapter(TrackItemRenderer trackRenderer, PlaylistItemRenderer playlistRenderer, FacebookInvitesItemRenderer facebookInvitesItemRenderer) {
        super(new CellRendererBinding<>(TRACK_ITEM_TYPE, trackRenderer),
                new CellRendererBinding<>(PLAYLIST_ITEM_TYPE, playlistRenderer),
                new CellRendererBinding<>(FACEBOOK_INVITES_ITEM_TYPE, facebookInvitesItemRenderer));
        this.trackRenderer = trackRenderer;
        this.facebookInvitesItemRenderer = facebookInvitesItemRenderer;
    }

    @Override
    public int getBasicItemViewType(int position) {
        StreamItem item = getItem(position);
        Urn urn = item.getEntityUrn();

        if (urn.isTrack()) {
            return TRACK_ITEM_TYPE;
        } else if (urn.isPlaylist()) {
            return PLAYLIST_ITEM_TYPE;
        } else if (urn.equals(FacebookInvitesItem.URN)) {
            return FACEBOOK_INVITES_ITEM_TYPE;
        } else {
            throw new IllegalArgumentException("unknown item type: " + item);
        }
    }

    public TrackItemRenderer getTrackRenderer() {
        return trackRenderer;
    }

    @Override
    protected SoundStreamViewHolder createViewHolder(View itemView) {
        return new SoundStreamViewHolder(itemView);
    }

    public static class SoundStreamViewHolder extends RecyclerView.ViewHolder {
        public SoundStreamViewHolder(View itemView) {
            super(itemView);
        }
    }

    public void setOnFacebookInvitesClickListener(FacebookInvitesItemRenderer.OnFacebookInvitesClickListener clickListener) {
        this.facebookInvitesItemRenderer.setOnFacebookInvitesClickListener(clickListener);
    }

}
