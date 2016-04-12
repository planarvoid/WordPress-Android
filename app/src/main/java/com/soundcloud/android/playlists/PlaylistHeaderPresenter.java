package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycles;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class PlaylistHeaderPresenter extends SupportFragmentLightCycleDispatcher<Fragment> {
    private final EventBus eventBus;
    private final PlaylistDetailsViewFactory playlistDetailsViewFactory;

    @LightCycle final PlaylistViewHeaderPresenter playlistViewHeaderPresenter;

    private Subscription foregroundSubscription = RxUtils.invalidSubscription();
    private Optional<PlaylistDetailsView> playlistDetailsView = Optional.absent();
    private Optional<PlaylistHeaderItem> playlist = Optional.absent();


    @Inject
    PlaylistHeaderPresenter(EventBus eventBus, PlaylistDetailsViewFactory playlistDetailsViewFactory, PlaylistViewHeaderPresenter playlistViewHeaderPresenter) {
        this.eventBus = eventBus;
        this.playlistDetailsViewFactory = playlistDetailsViewFactory;
        this.playlistViewHeaderPresenter = playlistViewHeaderPresenter;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        LightCycles.bind(this);
        super.onCreate(fragment, bundle);
    }

    @Override
    public void onViewCreated(final Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        final View headerView = view.findViewById(R.id.playlist_details);
        if (headerView != null) {
            bindView(headerView);
        }
    }

    @Override
    public void onResume(Fragment fragment) {
        super.onResume(fragment);
        foregroundSubscription = eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new PlaylistChangedSubscriber());
    }


    @Override
    public void onPause(Fragment fragment) {
        foregroundSubscription.unsubscribe();
        super.onPause(fragment);
    }


    public void setScreen(String screen) {
        playlistViewHeaderPresenter.setScreen(screen);
    }

    public void setListener(PlaylistHeaderListener playlistPresenter) {
        playlistViewHeaderPresenter.setListener(playlistPresenter);
    }

    public void setPlaylist(PlaylistHeaderItem playlistHeaderItem) {
        this.playlist = Optional.of(playlistHeaderItem);
        bindPlaylistToView();
    }

    public void bindView(View itemView) {
        playlistDetailsView = Optional.of(playlistDetailsViewFactory.create(itemView));
        playlistDetailsView.get().setOnPlayButtonClickListener(playlistViewHeaderPresenter.getPlayButtonListener());
        playlistDetailsView.get().setOnCreatorButtonClickListener(playlistViewHeaderPresenter.getCreatorClickListener());
        playlistViewHeaderPresenter.bindView(itemView);
        bindPlaylistToView();
    }

    private void bindPlaylistToView() {
        if (playlist.isPresent() && playlistDetailsView.isPresent()) {
            final PlaylistHeaderItem playlistHeaderItem = playlist.get();
            playlistDetailsView.get().setPlaylist(playlistHeaderItem, playlistHeaderItem.showPlayButton());
            playlistViewHeaderPresenter.setPlaylistInfo(playlistHeaderItem);
        }
    }

    public String getTitle() {
        return playlist.get().getTitle();
    }

    public boolean isPrivate() {
        return playlist.get().isPrivate();
    }

    private class PlaylistChangedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent event) {
            playlistViewHeaderPresenter.update(event);
        }
    }
}
