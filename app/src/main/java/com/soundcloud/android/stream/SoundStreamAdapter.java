package com.soundcloud.android.stream;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.facebookinvites.FacebookCreatorInvitesItemRenderer;
import com.soundcloud.android.facebookinvites.FacebookListenerInvitesItemRenderer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.stations.StationsOnboardingStreamItemRenderer;
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.upsell.StreamUpsellItemRenderer;
import com.soundcloud.android.upsell.UpsellItemRenderer;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class SoundStreamAdapter
        extends PagingRecyclerItemAdapter<SoundStreamItem, SoundStreamAdapter.SoundStreamViewHolder>
        implements PlayingTrackAware {

    private final FacebookListenerInvitesItemRenderer facebookListenerInvitesItemRenderer;
    private final StationsOnboardingStreamItemRenderer stationsOnboardingStreamItemRenderer;
    private final FacebookCreatorInvitesItemRenderer facebookCreatorInvitesItemRenderer;
    private final SuggestedCreatorsItemRenderer suggestedCreatorsItemRenderer;
    private final StreamUpsellItemRenderer upsellItemRenderer;

    @Inject
    public SoundStreamAdapter(StreamTrackItemRenderer trackItemRenderer,
                              StreamPlaylistItemRenderer playlistItemRenderer,
                              FacebookListenerInvitesItemRenderer facebookListenerInvitesItemRenderer,
                              StationsOnboardingStreamItemRenderer stationsOnboardingStreamItemRenderer,
                              FacebookCreatorInvitesItemRenderer facebookCreatorInvitesItemRenderer,
                              StreamUpsellItemRenderer upsellItemRenderer,
                              SuggestedCreatorsItemRenderer suggestedCreatorsItemRenderer) {
        super(new CellRendererBinding<>(SoundStreamItem.Kind.TRACK.ordinal(), trackItemRenderer),
              new CellRendererBinding<>(SoundStreamItem.Kind.PLAYLIST.ordinal(), playlistItemRenderer),
              new CellRendererBinding<>(SoundStreamItem.Kind.FACEBOOK_LISTENER_INVITES.ordinal(), facebookListenerInvitesItemRenderer),
              new CellRendererBinding<>(SoundStreamItem.Kind.STATIONS_ONBOARDING.ordinal(), stationsOnboardingStreamItemRenderer),
              new CellRendererBinding<>(SoundStreamItem.Kind.FACEBOOK_CREATORS.ordinal(), facebookCreatorInvitesItemRenderer),
              new CellRendererBinding<>(SoundStreamItem.Kind.STREAM_UPSELL.ordinal(), upsellItemRenderer),
              new CellRendererBinding<>(SoundStreamItem.Kind.SUGGESTED_CREATORS.ordinal(), suggestedCreatorsItemRenderer));
        this.facebookListenerInvitesItemRenderer = facebookListenerInvitesItemRenderer;
        this.facebookCreatorInvitesItemRenderer = facebookCreatorInvitesItemRenderer;
        this.stationsOnboardingStreamItemRenderer = stationsOnboardingStreamItemRenderer;
        this.upsellItemRenderer = upsellItemRenderer;
        this.suggestedCreatorsItemRenderer = suggestedCreatorsItemRenderer;
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).kind().ordinal();
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (SoundStreamItem viewModel : getItems()) {
            if (viewModel.kind() == SoundStreamItem.Kind.TRACK) {
                final TrackItem trackModel = ((SoundStreamItem.Track) viewModel).trackItem();
                trackModel.setIsPlaying(trackModel.getUrn().equals(currentlyPlayingUrn));
            }
        }
        notifyDataSetChanged();
    }

    void unsubscribe() {
        suggestedCreatorsItemRenderer.unsubscribe();
    }

    void onFollowingEntityChange(EntityStateChangedEvent event) {
        suggestedCreatorsItemRenderer.onFollowingEntityChange(event);
    }

    @Override
    protected SoundStreamViewHolder createViewHolder(View itemView) {
        return new SoundStreamViewHolder(itemView);
    }

    static class SoundStreamViewHolder extends RecyclerView.ViewHolder {
        SoundStreamViewHolder(View itemView) {
            super(itemView);
        }
    }

    void setOnFacebookInvitesClickListener(FacebookListenerInvitesItemRenderer.Listener clickListener) {
        this.facebookListenerInvitesItemRenderer.setListener(clickListener);
    }

    void setOnFacebookCreatorInvitesClickListener(FacebookCreatorInvitesItemRenderer.Listener clickListener) {
        this.facebookCreatorInvitesItemRenderer.setOnFacebookInvitesClickListener(clickListener);
    }

    void setOnStationsOnboardingStreamClickListener(StationsOnboardingStreamItemRenderer.Listener listener) {
        this.stationsOnboardingStreamItemRenderer.setListener(listener);
    }

    void setOnUpsellClickListener(UpsellItemRenderer.Listener listener) {
        this.upsellItemRenderer.setListener(listener);
    }
}
