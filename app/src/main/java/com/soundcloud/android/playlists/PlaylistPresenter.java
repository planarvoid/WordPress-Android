package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EntityStateChangedEvent.IS_TRACK_ADDED_TO_PLAYLIST_FILTER;
import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.events.EventQueue.OFFLINE_CONTENT_CHANGED;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_PROMOTED_SOURCE_INFO;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_QUERY_SOURCE_INFO;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_URN;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
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
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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

    @LightCycle PlaylistHeaderPresenter headerPresenter;

    private final PlaylistOperations playlistOperations;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final EventBus eventBus;
    private final Navigator navigator;
    private final PlaylistAdapter adapter;
    private final PlaybackInitiator playbackInitiator;

    protected PlaySessionSource playSessionSource;

    private CompositeSubscription eventSubscription = new CompositeSubscription();
    private Fragment fragment;

    public PlaylistPresenter(PlaylistOperations playlistOperations,
                             SwipeRefreshAttacher swipeRefreshAttacher,
                             PlaylistHeaderPresenterFactory headerPresenterFactory,
                             Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                             PlaylistAdapterFactory adapterFactory,
                             EventBus eventBus,
                             PlaybackInitiator playbackInitiator,
                             Navigator navigator) {
        super(swipeRefreshAttacher);
        this.playlistOperations = playlistOperations;
        this.navigator = navigator;
        this.headerPresenter = headerPresenterFactory.create(this);
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.adapter = adapterFactory.create(headerPresenter);
        this.eventBus = eventBus;
        this.playbackInitiator = playbackInitiator;
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

        eventSubscription.add(
                eventBus.queue(ENTITY_STATE_CHANGED)
                        .filter(IS_TRACK_ADDED_TO_PLAYLIST_FILTER)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new ReloadSubscriber()));

        eventSubscription.add(eventBus
                .queue(ENTITY_STATE_CHANGED)
                .filter(IS_CURRENT_PLAYLIST_DELETED)
                .subscribe(new GoBackSubscriber()));

        eventSubscription.add(eventBus
                .queue(ENTITY_STATE_CHANGED)
                .filter(IS_PLAYLIST_PUSHED_FILTER)
                .subscribe(new PlaylistPushedSubscriber()));

        eventSubscription.add(eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackSubscriber(adapter)));
        eventSubscription.add(eventBus.subscribe(ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)));
        eventSubscription.add(eventBus
                .subscribe(OFFLINE_CONTENT_CHANGED, new UpdateCurrentDownloadSubscriber(adapter)));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        eventSubscription.unsubscribe();
        eventSubscription = new CompositeSubscription();
        super.onDestroyView(fragment);
    }

    @Override
    public void onHeaderPlay() {
        playFromBeginning();
    }

    @Override
    public void onGoToCreator(Urn creatorUrn) {
        navigator.openProfile(fragment.getContext(), creatorUrn);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        playTracksAtPosition(position, expandPlayerSubscriberProvider.get());
    }

    private void playFromBeginning() {
        playTracksAtPosition(0, expandPlayerSubscriberProvider.get());
    }

    private void playTracksAtPosition(int trackPosition, Subscriber<PlaybackResult> playbackSubscriber) {
        playbackInitiator.playTracks(
                playlistOperations.trackUrnsForPlayback(getPlaylistUrn()),
                adapter.getItem(trackPosition).getUrn(), trackPosition, playSessionSource)
                .subscribe(playbackSubscriber);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    protected CollectionBinding<PlaylistWithTracks, ListItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(loadPlaylistObservable(fragmentArgs), getListItemTransformation())
                .withAdapter(adapter).build();
    }

    protected abstract Func1<PlaylistWithTracks, Iterable<ListItem>> getListItemTransformation();

    private Observable<PlaylistWithTracks> loadPlaylistObservable(Bundle fragmentArgs) {
        final Observable<PlaylistWithTracks> playlist = playlistOperations.playlist(getPlaylistUrn(fragmentArgs));
        return playlist.doOnNext(clearAdapter);
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

    private class ReloadSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent event) {
            reloadPlaylist();
        }
    }

    private void reloadPlaylist() {
        retryWith(CollectionBinding
                .from(loadPlaylistObservable(fragment.getArguments()), getListItemTransformation())
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
        }
    }

    public PlaySessionSource createPlaySessionSource(PlaylistWithTracks playlistWithTracks) {
        PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(
                Screen.fromBundle(fragment.getArguments()).get(),
                getPlaylistUrn(),
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
}
