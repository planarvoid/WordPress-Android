package com.soundcloud.android.stream;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AppInstallItemRenderer;
import com.soundcloud.android.ads.VideoAdItemRenderer;
import com.soundcloud.android.events.FollowingStatusEvent;
import com.soundcloud.android.facebookinvites.FacebookCreatorInvitesItemRenderer;
import com.soundcloud.android.facebookinvites.FacebookListenerInvitesItemRenderer;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsItemRenderer;
import com.soundcloud.android.upsell.StreamUpsellItemRenderer;
import com.soundcloud.android.upsell.UpsellItemRenderer;
import com.soundcloud.java.optional.Optional;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;
import java.util.ListIterator;

public class StreamAdapter extends PagingRecyclerItemAdapter<StreamItem, RecyclerView.ViewHolder> {

    private final FacebookListenerInvitesItemRenderer facebookListenerInvitesItemRenderer;
    private final FacebookCreatorInvitesItemRenderer facebookCreatorInvitesItemRenderer;
    private final SuggestedCreatorsItemRenderer suggestedCreatorsItemRenderer;
    private final StreamUpsellItemRenderer upsellItemRenderer;
    private final AppInstallItemRenderer appInstallItemRenderer;
    private final VideoAdItemRenderer videoAdItemRenderer;

    @Inject
    public StreamAdapter(StreamTrackItemRenderer trackItemRenderer,
                         StreamPlaylistItemRenderer playlistItemRenderer,
                         FacebookListenerInvitesItemRenderer facebookListenerInvitesItemRenderer,
                         FacebookCreatorInvitesItemRenderer facebookCreatorInvitesItemRenderer,
                         StreamUpsellItemRenderer upsellItemRenderer,
                         SuggestedCreatorsItemRenderer suggestedCreatorsItemRenderer,
                         AppInstallItemRenderer appInstallItemRenderer,
                         VideoAdItemRenderer videoAdItemRenderer) {
        super(new CellRendererBinding<>(StreamItem.Kind.TRACK.ordinal(), trackItemRenderer),
              new CellRendererBinding<>(StreamItem.Kind.PLAYLIST.ordinal(), playlistItemRenderer),
              new CellRendererBinding<>(StreamItem.Kind.FACEBOOK_LISTENER_INVITES.ordinal(), facebookListenerInvitesItemRenderer),
              new CellRendererBinding<>(StreamItem.Kind.FACEBOOK_CREATORS.ordinal(), facebookCreatorInvitesItemRenderer),
              new CellRendererBinding<>(StreamItem.Kind.STREAM_UPSELL.ordinal(), upsellItemRenderer),
              new CellRendererBinding<>(StreamItem.Kind.SUGGESTED_CREATORS.ordinal(), suggestedCreatorsItemRenderer),
              new CellRendererBinding<>(StreamItem.Kind.APP_INSTALL.ordinal(), appInstallItemRenderer),
              new CellRendererBinding<>(StreamItem.Kind.VIDEO_AD.ordinal(), videoAdItemRenderer));
        this.facebookListenerInvitesItemRenderer = facebookListenerInvitesItemRenderer;
        this.facebookCreatorInvitesItemRenderer = facebookCreatorInvitesItemRenderer;
        this.upsellItemRenderer = upsellItemRenderer;
        this.suggestedCreatorsItemRenderer = suggestedCreatorsItemRenderer;
        this.appInstallItemRenderer = appInstallItemRenderer;
        this.videoAdItemRenderer = videoAdItemRenderer;
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

    public void addItem(int position, StreamItem item) {
        if (position < getItemCount()) {
            items.add(position, item);
            notifyItemInserted(position);
        }
    }

    public void removeAds() {
        final ListIterator<StreamItem> iterator = items.listIterator();
        while (iterator.hasNext()) {
            if (iterator.next().isAd()) {
                iterator.remove();
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);

        if (cellRenderers.get(holder.getItemViewType()) instanceof VideoAdItemRenderer) {
            final Optional<AdData> item = getItem(holder.getAdapterPosition()).getAdData();
            videoAdItemRenderer.onViewAttachedToWindow(holder.itemView, item);
        }
    }

    public VideoAdItemRenderer getVideoAdItemRenderer() {
        return videoAdItemRenderer;
    }

    void onFollowingEntityChange(FollowingStatusEvent event) {
        suggestedCreatorsItemRenderer.onFollowingEntityChange(event);
    }

    void setOnFacebookInvitesClickListener(FacebookListenerInvitesItemRenderer.Listener clickListener) {
        this.facebookListenerInvitesItemRenderer.setListener(clickListener);
    }

    void setOnFacebookCreatorInvitesClickListener(FacebookCreatorInvitesItemRenderer.Listener clickListener) {
        this.facebookCreatorInvitesItemRenderer.setOnFacebookInvitesClickListener(clickListener);
    }

    void setOnUpsellClickListener(UpsellItemRenderer.Listener listener) {
        this.upsellItemRenderer.setListener(listener);
    }

    void setOnAppInstallClickListener(AppInstallItemRenderer.Listener listener) {
        this.appInstallItemRenderer.setListener(listener);
    }

    void setOnVideoAdClickListener(VideoAdItemRenderer.Listener listener) {
        this.videoAdItemRenderer.setListener(listener);
    }
}
