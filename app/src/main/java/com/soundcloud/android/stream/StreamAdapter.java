package com.soundcloud.android.stream;

import com.soundcloud.android.ads.AppInstallItemRenderer;
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

public class StreamAdapter
        extends PagingRecyclerItemAdapter<StreamItem, StreamAdapter.StreamViewHolder>
        implements PlayingTrackAware {

    private final FacebookListenerInvitesItemRenderer facebookListenerInvitesItemRenderer;
    private final StationsOnboardingStreamItemRenderer stationsOnboardingStreamItemRenderer;
    private final FacebookCreatorInvitesItemRenderer facebookCreatorInvitesItemRenderer;
    private final SuggestedCreatorsItemRenderer suggestedCreatorsItemRenderer;
    private final StreamUpsellItemRenderer upsellItemRenderer;
    private final AppInstallItemRenderer appInstallItemRenderer;

    @Inject
    public StreamAdapter(StreamTrackItemRenderer trackItemRenderer,
                         StreamPlaylistItemRenderer playlistItemRenderer,
                         FacebookListenerInvitesItemRenderer facebookListenerInvitesItemRenderer,
                         StationsOnboardingStreamItemRenderer stationsOnboardingStreamItemRenderer,
                         FacebookCreatorInvitesItemRenderer facebookCreatorInvitesItemRenderer,
                         StreamUpsellItemRenderer upsellItemRenderer,
                         SuggestedCreatorsItemRenderer suggestedCreatorsItemRenderer,
                         AppInstallItemRenderer appInstallItemRenderer) {
        super(new CellRendererBinding<>(StreamItem.Kind.TRACK.ordinal(), trackItemRenderer),
              new CellRendererBinding<>(StreamItem.Kind.PLAYLIST.ordinal(), playlistItemRenderer),
              new CellRendererBinding<>(StreamItem.Kind.FACEBOOK_LISTENER_INVITES.ordinal(), facebookListenerInvitesItemRenderer),
              new CellRendererBinding<>(StreamItem.Kind.STATIONS_ONBOARDING.ordinal(), stationsOnboardingStreamItemRenderer),
              new CellRendererBinding<>(StreamItem.Kind.FACEBOOK_CREATORS.ordinal(), facebookCreatorInvitesItemRenderer),
              new CellRendererBinding<>(StreamItem.Kind.STREAM_UPSELL.ordinal(), upsellItemRenderer),
              new CellRendererBinding<>(StreamItem.Kind.SUGGESTED_CREATORS.ordinal(), suggestedCreatorsItemRenderer),
              new CellRendererBinding<>(StreamItem.Kind.APP_INSTALL.ordinal(), appInstallItemRenderer));
        this.facebookListenerInvitesItemRenderer = facebookListenerInvitesItemRenderer;
        this.facebookCreatorInvitesItemRenderer = facebookCreatorInvitesItemRenderer;
        this.stationsOnboardingStreamItemRenderer = stationsOnboardingStreamItemRenderer;
        this.upsellItemRenderer = upsellItemRenderer;
        this.suggestedCreatorsItemRenderer = suggestedCreatorsItemRenderer;
        this.appInstallItemRenderer = appInstallItemRenderer;
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).kind().ordinal();
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (StreamItem viewModel : getItems()) {
            if (viewModel.kind() == StreamItem.Kind.TRACK) {
                final TrackItem trackModel = ((StreamItem.Track) viewModel).trackItem();
                trackModel.setIsPlaying(trackModel.getUrn().equals(currentlyPlayingUrn));
            }
        }
        notifyDataSetChanged();
    }

    void unsubscribe() {
        suggestedCreatorsItemRenderer.unsubscribe();
    }

    @Override
    protected StreamViewHolder createViewHolder(View itemView) {
        return new StreamViewHolder(itemView);
    }

    public void addItem(int position, StreamItem item) {
        if (position < getItemCount()) {
            items.add(position, item);
            notifyItemInserted(position);
        }
    }

    static class StreamViewHolder extends RecyclerView.ViewHolder {
        StreamViewHolder(View itemView) {
            super(itemView);
        }
    }

    void onFollowingEntityChange(EntityStateChangedEvent event) {
        suggestedCreatorsItemRenderer.onFollowingEntityChange(event);
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
