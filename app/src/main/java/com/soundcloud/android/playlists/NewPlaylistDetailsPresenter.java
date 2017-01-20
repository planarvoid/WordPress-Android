package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;

import android.content.res.Resources;

import java.util.Collections;
import java.util.List;

@AutoFactory
class NewPlaylistDetailsPresenter implements PlaylistDetailsViewListener {

    private final PlaylistOperations playlistOperations;
    private final LikesStateProvider likesStateProvider;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;
    private final TrackRepository trackRepository;
    private final Urn playlistUrn;
    private final BehaviorSubject<AsyncViewModel<PlaylistDetailsViewModel>> viewModelSubject = BehaviorSubject.create();
    private final BehaviorSubject<Boolean> refreshSubject = BehaviorSubject.create(false);
    private final BehaviorSubject<Boolean> editModeSubject = BehaviorSubject.create(false);
    private final Resources resources;
    private Subscription subscription = RxUtils.invalidSubscription();

    NewPlaylistDetailsPresenter(@Provided PlaylistOperations playlistOperations,
                                @Provided LikesStateProvider likesStateProvider,
                                @Provided SyncInitiator syncInitiator,
                                @Provided EventBus eventBus,
                                @Provided TrackRepository trackRepository,
                                @Provided Resources resources,
                                Urn playlistUrn) {
        this.playlistOperations = playlistOperations;
        this.likesStateProvider = likesStateProvider;
        this.syncInitiator = syncInitiator;
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
        this.playlistUrn = playlistUrn;
        this.resources = resources;
    }

    public void connect() {
        subscription.unsubscribe();
        subscription = Observable
                .combineLatest(refreshSubject, editModeSubject, playlist(), tracks(), likedStatus(), this::combine)
                .doOnNext(viewModelSubject::onNext)
                .subscribe(new DefaultSubscriber<>());
    }

    @Override
    public void onHeaderPlayButtonClicked() {
//        playbackInitiator.playTracks(
//                playlistOperations.trackUrnsForPlayback(playSessionSource.getCollectionUrn()),
//                ((PlaylistDetailTrackItem) adapter.getItem(position)).getUrn(), position - (addInlineHeader ? 1 : 0), playSessionSource)
//                         .subscribe(expandPlayerSubscriberProvider.get());
    }

    @Override
    public void onCreatorClicked() {
//        navigator.legacyOpenProfile(fragment.getActivity(), headerItemOpt.get().creatorUrn());
    }

    @Override
    public void onEnterEditMode() {
        editModeSubject.onNext(true);
    }

    @Override
    public void onExitEditMode() {
        editModeSubject.onNext(false);
    }

    private Observable<List<TrackItem>> tracks() {
        return trackRepository
                .forPlaylist(playlistUrn)
                // TODO : It emits asap - how to handle the loading spinner?
                .startWith(Collections.<TrackItem>emptyList());
    }

    private Observable<LikedStatuses> likedStatus() {
        return likesStateProvider.likedStatuses()
                                 .distinctUntilChanged(likedStatuses -> likedStatuses.isLiked(playlistUrn));
    }

    Observable<Playlist> playlist() {
        return eventBus.queue(EventQueue.PLAYLIST_CHANGED)
                       .filter(event -> event.changeMap().containsKey(playlistUrn))
                       .flatMap(ignored -> playlistOperations.playlist(playlistUrn))
                       .startWith(playlistOperations.playlist(playlistUrn));
    }

    public void refresh() {
        syncInitiator.syncPlaylist(playlistUrn)
                     .subscribe(new DefaultSubscriber<SyncJobResult>() {
                         // TODO : errors
                         @Override
                         public void onStart() {
                             refreshSubject.onNext(true);
                         }

                         @Override
                         public void onCompleted() {
                             refreshSubject.onNext(false);
                         }
                     });
    }

    private AsyncViewModel<PlaylistDetailsViewModel> combine(Boolean isRefreshing,
                                                             Boolean isEditMode,
                                                             Playlist playlist,
                                                             List<TrackItem> tracks,
                                                             LikedStatuses likedStatuses) {
        final boolean isLiked = likedStatuses.isLiked(playlist.urn());
        final PlaylistDetailsViewModel model = PlaylistDetailsViewModel.from(playlist, tracks, isLiked, isEditMode, resources);
        return AsyncViewModel.create(model, isRefreshing);
    }

    public void disconnect() {
        subscription.unsubscribe();
    }

    public Observable<AsyncViewModel<PlaylistDetailsViewModel>> viewModel() {
        return viewModelSubject;
    }

    /**
     * TODO :
     * - Upsells
     * - Other playlists by user
     * - Local playlist pushed to server while viewing it
     * - missing playlists
     */
}
