package com.soundcloud.android.playlists;

import static com.soundcloud.java.collections.Lists.transform;

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
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;

import java.util.List;

@AutoFactory
class NewPlaylistDetailsPresenter {

    private final PlaylistOperations playlistOperations;
    private final LikesStateProvider likesStateProvider;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;
    private final Urn playlistUrn;
    private final BehaviorSubject<AsyncViewModel<PlaylistDetailsViewModel>> viewModelSubject = BehaviorSubject.create();
    private final BehaviorSubject<Boolean> refreshSubject = BehaviorSubject.create(false);
    private Subscription subscription = RxUtils.invalidSubscription();

    NewPlaylistDetailsPresenter(@Provided PlaylistOperations playlistOperations,
                                @Provided LikesStateProvider likesStateProvider,
                                @Provided SyncInitiator syncInitiator,
                                @Provided EventBus eventBus,
                                Urn playlistUrn) {
        this.playlistOperations = playlistOperations;
        this.likesStateProvider = likesStateProvider;
        this.syncInitiator = syncInitiator;
        this.eventBus = eventBus;
        this.playlistUrn = playlistUrn;
    }

    public void connect() {
        subscription.unsubscribe();
        subscription = Observable
                .combineLatest(refreshSubject, playlist(), likedStatus(), this::combine)
                .doOnNext(viewModelSubject::onNext)
                .subscribe(new DefaultSubscriber<>());
    }

    private Observable<LikedStatuses> likedStatus() {
        return likesStateProvider.likedStatuses()
                          .distinctUntilChanged(likedStatuses -> likedStatuses.isLiked(playlistUrn));
    }

    Observable<PlaylistWithTracks> playlist() {
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
                                                             PlaylistWithTracks playlistWithTracks,
                                                             LikedStatuses likedStatuses) {
        final boolean isLiked = likedStatuses.isLiked(playlistWithTracks.getUrn());
        final PlaylistDetailsViewModel model = PlaylistDetailsViewModel.create(playlistWithTracks,
                                                                              toListItems(playlistWithTracks),
                                                                              isLiked);
        return AsyncViewModel.create(model, isRefreshing);
    }

    public void disconnect() {
        subscription.unsubscribe();
    }

    public Observable<AsyncViewModel<PlaylistDetailsViewModel>> viewModel() {
        return viewModelSubject;
    }


    private List<PlaylistDetailItem> toListItems(PlaylistWithTracks playlistWithTracks) {
        return transform(playlistWithTracks.getTracks(), PlaylistDetailTrackItem::new);
    }

    /**
     * TODO :
     * - Upsells
     * - Other playlists by user
     * - Local playlist pushed to server while viewing it
     */
}
