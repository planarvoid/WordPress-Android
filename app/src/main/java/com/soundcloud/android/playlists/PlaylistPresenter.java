package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EntityStateChangedEvent.IS_TRACK_ADDED_TO_PLAYLIST_FILTER;
import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.events.EventQueue.OFFLINE_CONTENT_CHANGED;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_PROMOTED_SOURCE_INFO;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_QUERY_SOURCE_INFO;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_URN;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Provider;

public abstract class PlaylistPresenter extends RecyclerViewPresenter<PlaylistWithTracks, ListItem> implements PlaylistHeaderListener {

    private final Func1<EntityStateChangedEvent, Boolean> IS_CURRENT_PLAYLIST_DELETED = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.getKind() == EntityStateChangedEvent.ENTITY_DELETED
                    && event.getFirstUrn().equals(getPlaylistUrn());
        }
    };

    private final Func1<EntityStateChangedEvent, Boolean> IS_PLAYLIST_PUSHED_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.getKind() == EntityStateChangedEvent.PLAYLIST_PUSHED_TO_SERVER
                    && event.getFirstUrn().equals(getPlaylistUrn());
        }
    };

    private final Action1<PlaylistWithTracks> clearAdapter = new Action1<PlaylistWithTracks>() {
        @Override
        public void call(PlaylistWithTracks playlistWithTracks) {
            adapter.clear();
        }
    };

    @LightCycle final PlaylistHeaderPresenter headerPresenter;
    private final ViewStrategyFactory viewStrategyFactory;
    private final PlaylistOperations playlistOperations;
    private final EventBus eventBus;
    private final Navigator navigator;
    private final PlaylistAdapter adapter;

    protected PlaySessionSource playSessionSource;
    private Fragment fragment;
    private Subscription subscription = RxUtils.invalidSubscription();
    private PlaylistStrategy strategy;
    private String screen;

    public PlaylistPresenter(PlaylistOperations playlistOperations,
                             SwipeRefreshAttacher swipeRefreshAttacher,
                             PlaylistHeaderPresenter headerPresenter,
                             PlaylistAdapterFactory adapterFactory,
                             EventBus eventBus,
                             Navigator navigator,
                             ViewStrategyFactory viewStrategyFactory) {
        super(swipeRefreshAttacher);
        this.playlistOperations = playlistOperations;
        this.eventBus = eventBus;
        this.navigator = navigator;
        this.headerPresenter = headerPresenter;
        this.viewStrategyFactory = viewStrategyFactory;
        headerPresenter.setListener(this);
        this.adapter = adapterFactory.create(headerPresenter);
        this.strategy = PlaylistStrategy.EMPTY;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        this.fragment = fragment;
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        subscription = new CompositeSubscription(
                eventBus
                        .queue(ENTITY_STATE_CHANGED)
                        .filter(IS_CURRENT_PLAYLIST_DELETED)
                        .subscribe(new GoBackSubscriber()),
                eventBus
                        .queue(ENTITY_STATE_CHANGED)
                        .filter(IS_PLAYLIST_PUSHED_FILTER)
                        .subscribe(new PlaylistPushedSubscriber()));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        subscription.unsubscribe();
        strategy.stop();
        super.onDestroyView(fragment);
    }

    @Override
    public void onHeaderPlay() {
        strategy.onHeaderClick();
    }

    @Override
    public void onGoToCreator(Urn creatorUrn) {
        navigator.openProfile(fragment.getContext(), creatorUrn);
    }

    @Override
    public void onEditPlaylist() {
        changeStrategy(new EditStrategy(eventBus, this));
    }

    private void onViewPlaylist() {
        changeStrategy(viewStrategyFactory.create(PlaylistPresenter.this, adapter, playSessionSource));
    }

    private void startEditMode() {
        fragment.getActivity().startActionMode(new EditModeCallback());
    }

    private void leaveEditMode() {
        //TODO tracks changed
        //if (tracks changed) {
        //TODO add tracks
        //fireAndForget(playlistOperations.editPlaylist(getPlaylistUrn(), headerPresenter.getTitle(), headerPresenter.isPrivate(), null));
        //}
        onViewPlaylist();
    }

    private void discardChanged() {
        //TODO discard changes of tracks
    }

    private void changeStrategy(PlaylistStrategy newStrategy) {
        strategy.stop();
        strategy = newStrategy;
        strategy.start();
    }

    @Override
    protected void onItemClicked(View view, int position) {
        strategy.onItemClicked(position);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    protected CollectionBinding<PlaylistWithTracks, ListItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(loadPlaylistObservable(getPlaylistUrn(fragmentArgs)), getListItemTransformation())
                .withAdapter(adapter).build();
    }

    protected abstract Func1<PlaylistWithTracks, Iterable<ListItem>> getListItemTransformation();

    private Observable<PlaylistWithTracks> loadPlaylistObservable(Urn playlistUrn) {
        return playlistOperations.playlist(playlistUrn).doOnNext(clearAdapter);
    }

    private Urn getPlaylistUrn(Bundle fragmentArgs) {
        return fragmentArgs.getParcelable(EXTRA_URN);
    }

    private Urn getPlaylistUrn() {
        return fragment.getArguments().getParcelable(EXTRA_URN);
    }

    private SearchQuerySourceInfo getSearchQuerySourceInfo() {
        return fragment.getArguments().getParcelable(EXTRA_QUERY_SOURCE_INFO);
    }

    @Override
    protected void onSubscribeBinding(CollectionBinding<PlaylistWithTracks, ListItem> collectionBinding, CompositeSubscription viewLifeCycle) {
        collectionBinding.source().subscribe(getPlaylistSubscriber());
    }

    @NonNull
    protected PlaylistSubscriber getPlaylistSubscriber() {
        return new PlaylistSubscriber();
    }

    @Override
    protected CollectionBinding<PlaylistWithTracks, ListItem> onRefreshBinding() {
        final Urn playlistUrn = getPlaylistUrn(fragment.getArguments());
        return CollectionBinding.from(playlistOperations.updatedPlaylistInfo(playlistUrn), getListItemTransformation())
                .withAdapter(adapter)
                .build();
    }

    private PromotedSourceInfo getPromotedSourceInfo() {
        return (PromotedSourceInfo) fragment.getArguments().getParcelable(EXTRA_PROMOTED_SOURCE_INFO);
    }

    void setScreen(String screen) {
        this.screen = screen;
        headerPresenter.setScreen(screen);
    }

    void reloadPlaylist() {
        retryWith(CollectionBinding
                .from(loadPlaylistObservable(getPlaylistUrn()), getListItemTransformation())
                .withAdapter(adapter).build());
    }

    private class GoBackSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent args) {
            // This actually go back in the stack because the fragment is tied up
            // to PlaylistDetailActivity.
            if (fragment.isAdded()) {
                fragment.getActivity().finish();
            }
        }
    }

    protected class PlaylistSubscriber extends DefaultSubscriber<PlaylistWithTracks> {
        @Override
        public void onNext(PlaylistWithTracks playlistWithTracks) {
            playSessionSource = createPlaySessionSource(playlistWithTracks);
            onViewPlaylist();
        }
    }

    protected PlaySessionSource createPlaySessionSource(PlaylistWithTracks playlistWithTracks) {
        PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(
                screen,
                playlistWithTracks.getUrn(),
                playlistWithTracks.getCreatorUrn(),
                playlistWithTracks.getTrackCount());

        PromotedSourceInfo promotedSourceInfo = getPromotedSourceInfo();
        SearchQuerySourceInfo searchQuerySourceInfo = getSearchQuerySourceInfo();

        if (promotedSourceInfo != null) {
            playSessionSource.setPromotedSourceInfo(promotedSourceInfo);
        } else if (searchQuerySourceInfo != null) {
            playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        }
        return playSessionSource;
    }

    private class PlaylistPushedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent args) {
            final PropertySet updatedPlaylist = args.getNextChangeSet();
            fragment.getArguments().putParcelable(EXTRA_URN, updatedPlaylist.get(PlaylistProperty.URN));
        }
    }

    private final class EditModeCallback implements ActionMode.Callback {
        public static final int DISCARD_ITEM_ID = 1;
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add(0, DISCARD_ITEM_ID, Menu.NONE, R.string.btn_cancel);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
            if (item.getItemId() == DISCARD_ITEM_ID) {
                discardChanged();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            leaveEditMode();
        }
    }

    private interface PlaylistStrategy {

        PlaylistStrategy EMPTY = new PlaylistStrategy() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public void onItemClicked(int position) {
            }

            @Override
            public void onHeaderClick() {
            }
        };

        void start();

        void stop();

        void onItemClicked(int position);

        void onHeaderClick();
    }

    @AutoFactory
    static class ViewStrategy implements PlaylistStrategy {

        private final PlaylistPresenter presenter;
        private final EventBus eventBus;
        private final PlaybackInitiator playbackInitiator;
        private final PlaylistOperations playlistOperations;
        private final PlaylistAdapter adapter;
        private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
        private PlaySessionSource playSessionSource;

        private Subscription eventSubscription = RxUtils.invalidSubscription();

        ViewStrategy(@Provided EventBus eventBus,
                     @Provided PlaybackInitiator playbackInitiator,
                     @Provided PlaylistOperations playlistOperations,
                     @Provided Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                     PlaylistPresenter presenter,
                     PlaylistAdapter adapter,
                     PlaySessionSource playSessionSource) {
            this.presenter = presenter;
            this.eventBus = eventBus;
            this.playbackInitiator = playbackInitiator;
            this.playlistOperations = playlistOperations;
            this.adapter = adapter;
            this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
            this.playSessionSource = playSessionSource;
        }

        @Override
        public void start() {
            eventSubscription = new CompositeSubscription(
                    eventBus.queue(ENTITY_STATE_CHANGED)
                            .filter(IS_TRACK_ADDED_TO_PLAYLIST_FILTER)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new ReloadSubscriber()),
                    eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM,
                            new UpdatePlayingTrackSubscriber(adapter)),
                    eventBus.subscribe(ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)),
                    eventBus.subscribe(OFFLINE_CONTENT_CHANGED, new UpdateCurrentDownloadSubscriber(adapter)));
        }

        @Override
        public void stop() {
            eventSubscription.unsubscribe();
        }

        @Override
        public void onItemClicked(int position) {
            playTracksAtPosition(position, expandPlayerSubscriberProvider.get());
        }

        @Override
        public void onHeaderClick() {
            playFromBeginning();
        }

        private void playFromBeginning() {
            playTracksAtPosition(0, expandPlayerSubscriberProvider.get());
        }

        private void playTracksAtPosition(int trackPosition, Subscriber<PlaybackResult> playbackSubscriber) {
            playbackInitiator.playTracks(
                    playlistOperations.trackUrnsForPlayback(playSessionSource.getCollectionUrn()),
                    adapter.getItem(trackPosition).getUrn(), trackPosition, playSessionSource)
                    .subscribe(playbackSubscriber);
        }

        private class ReloadSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
            @Override
            public void onNext(EntityStateChangedEvent event) {
                presenter.reloadPlaylist();
            }
        }

    }

    private static class EditStrategy implements PlaylistStrategy {
        private final EventBus eventBus;
        private final PlaylistPresenter presenter;

        private EditStrategy(EventBus eventBus,
                             PlaylistPresenter presenter) {
            this.eventBus = eventBus;
            this.presenter = presenter;
        }

        @Override
        public void start() {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.hidePlayer());
            presenter.startEditMode();
        }

        @Override
        public void stop() {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
        }

        @Override
        public void onItemClicked(int position) {

        }

        @Override
        public void onHeaderClick() {

        }
    }
}
