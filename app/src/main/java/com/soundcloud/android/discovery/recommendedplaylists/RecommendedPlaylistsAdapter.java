package com.soundcloud.android.discovery.recommendedplaylists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.discovery.recommendations.QuerySourceInfo;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory
public class RecommendedPlaylistsAdapter extends RecyclerItemAdapter<PlaylistItem, RecyclerView.ViewHolder>
        implements CarouselPlaylistItemRenderer.PlaylistListener {

    private static final String PLAYLIST_DISCOVERY_SOURCE = "playlist_discovery";

    public interface QueryPositionProvider {
        int queryPosition(String bucketKey, int bucketPosition);
    }

    private static final int RECOMMENDED_PLAYLIST_TYPE = 0;

    private final QueryPositionProvider queryPositionProvider;
    private final Navigator navigator;
    private final ScreenProvider screenProvider;
    private final EventBus eventBus;

    private Optional<String> key = Optional.absent();
    private Optional<Urn> queryUrn = Optional.absent();

    RecommendedPlaylistsAdapter(QueryPositionProvider queryPositionProvider,
                                @Provided CarouselPlaylistItemRenderer renderer,
                                @Provided Navigator navigator,
                                @Provided ScreenProvider screenProvider,
                                @Provided EventBus eventBus) {
        super(renderer);
        this.queryPositionProvider = queryPositionProvider;
        this.navigator = navigator;
        this.screenProvider = screenProvider;
        this.eventBus = eventBus;
        renderer.setPlaylistListener(this);
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return RECOMMENDED_PLAYLIST_TYPE;
    }

    boolean hasBucketItem() {
        return key.isPresent();
    }

    String bucketId() {
        return key.get();
    }

    void setRecommendedTracksBucketItem(RecommendedPlaylistsBucketItem recommendedPlaylists) {
        key = Optional.of(recommendedPlaylists.key());
        queryUrn = recommendedPlaylists.queryUrn();
        clear();
        onNext(recommendedPlaylists.playlists());
    }

    @Override
    public void onPlaylistClick(Context context, PlaylistItem playlist, int position) {
        final Urn playlistUrn = playlist.getUrn();
        final Screen screen = screenProvider.getLastScreen();

        final Module module = Module.create(Module.RECOMMENDED_PLAYLISTS, position);
        final Optional<Integer> queryPosition = key.isPresent() ?
                                                Optional.of(queryPositionProvider.queryPosition(key.get(), position)) :
                                                Optional.absent();
        final EventContextMetadata eventContextMetadata = getEventContextMetadata(module,
                                                                                  screen,
                                                                                  queryPosition,
                                                                                  this.queryUrn);
        final UIEvent event = UIEvent.fromNavigation(playlistUrn, eventContextMetadata);
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromRecommendedPlaylists(playlistUrn, eventContextMetadata));
        navigator.openPlaylist(context, playlistUrn, screen, event);
    }

    private EventContextMetadata getEventContextMetadata(Module module,
                                                         Screen screen,
                                                         Optional<Integer> queryPosition,
                                                         Optional<Urn> queryUrn) {
        final EventContextMetadata.Builder builder = EventContextMetadata.builder().pageName(screen.get());
        final TrackSourceInfo sourceInfo = new TrackSourceInfo(screen.get(), true);
        if (queryUrn.isPresent() && queryPosition.isPresent()) {
            sourceInfo.setQuerySourceInfo(QuerySourceInfo.create(queryPosition.get(),
                                                                 queryUrn.get()));
        }
        sourceInfo.setSource(PLAYLIST_DISCOVERY_SOURCE, Strings.EMPTY);
        builder.trackSourceInfo(sourceInfo);
        builder.module(module);
        return builder.build();
    }
}
