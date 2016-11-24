package com.soundcloud.android.stream;

import com.soundcloud.android.ads.AppInstallItemRenderer;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.facebookinvites.FacebookCreatorInvitesItemRenderer;
import com.soundcloud.android.facebookinvites.FacebookListenerInvitesItemRenderer;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.stations.StationsOnboardingStreamItemRenderer;
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsItemRenderer;
import com.soundcloud.android.upsell.StreamUpsellItemRenderer;
import com.soundcloud.android.upsell.UpsellItemRenderer;
import com.soundcloud.java.checks.Preconditions;

import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class StreamAdapter extends PagingRecyclerItemAdapter<StreamItem, RecyclerView.ViewHolder> {

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

    void unsubscribe() {
        suggestedCreatorsItemRenderer.unsubscribe();
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public void addItem(StreamItem item) {
        checkMainThread();
        super.addItem(item);
    }

    public void addItem(int position, StreamItem item) {
        checkMainThread();
        if (position < getItemCount()) {
            items.add(position, item);
            notifyItemInserted(position);
        }
    }

    @Override
    public void clear() {
        checkMainThread();
        super.clear();
    }

    @Override
    public void prependItem(StreamItem item) {
        checkMainThread();
        super.prependItem(item);
    }

    @Override
    public void removeItem(int position) {
        checkMainThread();
        super.removeItem(position);
    }

    // debugging https://github.com/soundcloud/SoundCloud-Android/issues/6323
    private void checkMainThread() {
        Preconditions.checkState(Looper.myLooper() == Looper.getMainLooper(),
                                 "Altering adapter off main thread");
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

    void setOnAppInstallClickListener(AppInstallItemRenderer.Listener listener) {
        this.appInstallItemRenderer.setListener(listener);
    }
}
