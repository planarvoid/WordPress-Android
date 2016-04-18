package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EntityStateChangedEvent.IS_TRACK_ADDED_TO_PLAYLIST_FILTER;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class PlaylistContentPresenter extends DefaultSupportFragmentLightCycle<Fragment> {
    private final PlaylistEditViewFactory playlistEditViewFactory;
    private final PlaylistDefaultViewFactory playlistDefaultViewFactory;

    private PlaylistContentView playlistContentView = PlaylistContentView.EMPTY;

    @Inject
    PlaylistContentPresenter(PlaylistEditViewFactory playlistEditViewFactory,
                             PlaylistDefaultViewFactory playlistDefaultViewFactory) {
        this.playlistEditViewFactory = playlistEditViewFactory;
        this.playlistDefaultViewFactory = playlistDefaultViewFactory;
    }

    @Override
    public void onStart(Fragment fragment) {
        super.onStart(fragment);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        playlistContentView.start();
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        playlistContentView.stop();
    }

    public void setEditMode(PlaylistPresenter playlistPresenter, boolean isEditMode) {
        if (isEditMode) {
            switchViewMode(playlistEditViewFactory.create(playlistPresenter));
        } else {
            switchViewMode(playlistDefaultViewFactory.create(playlistPresenter));
        }
    }

    public void onItemClicked(int position) {
        playlistContentView.onItemClicked(position);
    }

    private void switchViewMode(PlaylistContentView newView) {
        playlistContentView.stop();
        playlistContentView = newView;
        playlistContentView.start();
    }

    interface PlaylistContentView {

        PlaylistContentView EMPTY = new PlaylistContentView() {
            @Override
            public void start() { }

            @Override
            public void stop() { }

            @Override
            public void onItemClicked(int position) { }

            @Override
            public void onHeaderClick() { }
        };

        void start();

        void stop();

        void onItemClicked(int position);

        void onHeaderClick();
    }

    @AutoFactory
    static class PlaylistDefaultView implements PlaylistContentView {

        private final PlaylistPresenter presenter;
        private final EventBus eventBus;
        private Subscription eventSubscription = RxUtils.invalidSubscription();

        PlaylistDefaultView(@Provided EventBus eventBus, PlaylistPresenter presenter) {
            this.presenter = presenter;
            this.eventBus = eventBus;
        }

        @Override
        public void start() {
            eventSubscription = eventBus.queue(ENTITY_STATE_CHANGED)
                    .filter(IS_TRACK_ADDED_TO_PLAYLIST_FILTER)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new ReloadSubscriber());
        }

        @Override
        public void stop() {
            eventSubscription.unsubscribe();
        }

        @Override
        public void onItemClicked(int position) {
            presenter.play(position);
        }

        @Override
        public void onHeaderClick() {
            presenter.play(0);
        }

        private class ReloadSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
            @Override
            public void onNext(EntityStateChangedEvent event) {
                presenter.reloadPlaylist();
            }
        }
    }

    @AutoFactory
    static class PlaylistEditView implements PlaylistContentView {

        private final EventBus eventBus;
        private final PlaylistPresenter presenter;

        PlaylistEditView(@Provided EventBus eventBus,
                         PlaylistPresenter presenter) {
            this.eventBus = eventBus;
            this.presenter = presenter;
        }

        @Override
        public void start() {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.hidePlayer());
            presenter.showEditMode();
        }

        @Override
        public void stop() {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
            presenter.savePlaylist();
        }

        @Override
        public void onItemClicked(int position) {
            // no-op
        }

        @Override
        public void onHeaderClick() {
            // no-op
        }
    }

}
