package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.discovery.recommendedplaylists.CarouselPlaylistItemRenderer;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory
class OtherPlaylistsByUserAdapter extends RecyclerItemAdapter<PlaylistItem, RecyclerView.ViewHolder>
        implements CarouselPlaylistItemRenderer.PlaylistListener {

    private final ScreenProvider screenProvider;
    private final EventBus eventBus;
    private final Navigator navigator;

    OtherPlaylistsByUserAdapter(@Provided CarouselPlaylistItemRenderer renderer,
                                @Provided ScreenProvider screenProvider,
                                @Provided EventBus eventBus,
                                @Provided Navigator navigator) {
        super(renderer);
        renderer.setPlaylistListener(this);
        this.screenProvider = screenProvider;
        this.eventBus = eventBus;
        this.navigator = navigator;
    }

    void setOtherPlaylistsByUser(PlaylistDetailOtherPlaylistsItem otherPlaylistsItem) {
        clear();
        onNext(otherPlaylistsItem.otherPlaylists());
        notifyDataSetChanged();
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public void onPlaylistClick(Context context, PlaylistItem playlist, int position) {
        final Urn playlistUrn = playlist.getUrn();
        final Screen screen = screenProvider.getLastScreen();

        final Module module = Module.create(Module.MORE_PLAYLISTS_BY_USER, position);

        final EventContextMetadata eventContextMetadata = getEventContextMetadata(module, screen);
        final UIEvent event = UIEvent.fromNavigation(playlistUrn, eventContextMetadata);
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromMorePlaylistsByUser(playlistUrn, eventContextMetadata));
        navigator.openPlaylist(context, playlistUrn, screen, event);
    }

    private EventContextMetadata getEventContextMetadata(Module module,
                                                         Screen screen) {
        final EventContextMetadata.Builder builder = EventContextMetadata.builder().pageName(screen.get());
        builder.trackSourceInfo(new TrackSourceInfo(screen.get(), true));
        builder.module(module);
        return builder.build();
    }

    @Override
    public int getBasicItemViewType(int position) {
        return 0;
    }
}
