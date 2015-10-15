package com.soundcloud.android.stream;

import com.soundcloud.android.facebookinvites.FacebookInvitesItem;
import com.soundcloud.android.facebookinvites.FacebookInvitesItemRenderer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.stations.StationOnboardingStreamItem;
import com.soundcloud.android.stations.StationsOnboardingStreamItemRenderer;
import com.soundcloud.android.view.adapters.NowPlayingAdapter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class SoundStreamAdapter
        extends PagingRecyclerItemAdapter<StreamItem, SoundStreamAdapter.SoundStreamViewHolder>
        implements NowPlayingAdapter {

    private static final int TRACK_ITEM_TYPE = 0;
    private static final int PLAYLIST_ITEM_TYPE = 1;
    private static final int FACEBOOK_INVITES_ITEM_TYPE = 2;
    private static final int STATIONS_ONBOARDING_STREAM_ITEM_TYPE = 3;

    private final FacebookInvitesItemRenderer facebookInvitesItemRenderer;
    private final StationsOnboardingStreamItemRenderer stationsOnboardingStreamItemRenderer;

    @Inject
    public SoundStreamAdapter(TrackItemRenderer trackRenderer, PlaylistItemRenderer playlistRenderer, FacebookInvitesItemRenderer facebookInvitesItemRenderer,
                              StationsOnboardingStreamItemRenderer stationsOnboardingStreamItemRenderer) {
        super(new CellRendererBinding<>(TRACK_ITEM_TYPE, trackRenderer),
                new CellRendererBinding<>(PLAYLIST_ITEM_TYPE, playlistRenderer),
                new CellRendererBinding<>(FACEBOOK_INVITES_ITEM_TYPE, facebookInvitesItemRenderer),
                new CellRendererBinding<>(STATIONS_ONBOARDING_STREAM_ITEM_TYPE, stationsOnboardingStreamItemRenderer));
        this.facebookInvitesItemRenderer = facebookInvitesItemRenderer;
        this.stationsOnboardingStreamItemRenderer = stationsOnboardingStreamItemRenderer;
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
        } else if (urn.equals(StationOnboardingStreamItem.URN)) {
            return STATIONS_ONBOARDING_STREAM_ITEM_TYPE;
        } else {
            throw new IllegalArgumentException("unknown item type: " + item);
        }
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (StreamItem viewModel : getItems()) {
            if (viewModel instanceof TrackItem) {
                final TrackItem trackModel = (TrackItem) viewModel;
                trackModel.setIsPlaying(trackModel.getEntityUrn().equals(currentlyPlayingUrn));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    protected SoundStreamViewHolder createViewHolder(View itemView) {
        return new SoundStreamViewHolder(itemView);
    }

    static class SoundStreamViewHolder extends RecyclerView.ViewHolder {
        public SoundStreamViewHolder(View itemView) {
            super(itemView);
        }
    }

    void setOnFacebookInvitesClickListener(FacebookInvitesItemRenderer.OnFacebookInvitesClickListener clickListener) {
        this.facebookInvitesItemRenderer.setOnFacebookInvitesClickListener(clickListener);
    }

    void setOnStationsOnboardingStreamClickListener(StationsOnboardingStreamItemRenderer.Listener listener) {
        this.stationsOnboardingStreamItemRenderer.setListener(listener);
    }

}
