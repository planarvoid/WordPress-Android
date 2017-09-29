package com.soundcloud.android.playlists;

import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.collection.ConfirmRemoveOfflineDialogFragment;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikeToggleObserver;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.presentation.ItemMenuOptions;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultCompletableObserver;
import com.soundcloud.android.rx.observers.DefaultMaybeObserver;
import com.soundcloud.android.settings.OfflineStorageErrorDialog;
import com.soundcloud.android.share.SharePresenter;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.view.View;

import javax.inject.Inject;

public class PlaylistItemMenuPresenter implements PlaylistItemMenuRenderer.Listener {

    private final Context appContext;
    private final EventBus eventBus;
    private final PlaylistRepository playlistRepository;
    private final LikeOperations likeOperations;
    private final RepostOperations repostOperations;
    private final SharePresenter sharePresenter;
    private final ScreenProvider screenProvider;
    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final NavigationExecutor navigationExecutor;
    private final Navigator navigator;
    private final PlayQueueHelper playQueueHelper;
    private final EventTracker eventTracker;
    private final PlaylistItemMenuRendererFactory playlistItemMenuRendererFactory;
    private final AccountOperations accountOperations;
    private final EntityItemCreator entityItemCreator;
    private final OfflineSettingsStorage offlineSettingsStorage;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;
    private final FeedbackController feedbackController;

    private Disposable playlistDisposable = RxUtils.invalidDisposable();
    private Optional<EventContextMetadata.Builder> eventContextMetadataBuilder;
    private Urn playlistUrn;

    private Optional<PromotedSourceInfo> promotedSourceInfo;
    private EntityMetadata entityMetadata;
    private PlaylistItemMenuRenderer renderer;
    private boolean isShowing;
    private ItemMenuOptions itemMenuOptions;

    @Inject
    public PlaylistItemMenuPresenter(Context appContext,
                                     EventBus eventBus,
                                     PlaylistRepository playlistRepository,
                                     LikeOperations likeOperations,
                                     RepostOperations repostOperations,
                                     SharePresenter sharePresenter,
                                     ScreenProvider screenProvider,
                                     FeatureOperations featureOperations,
                                     OfflineContentOperations offlineContentOperations,
                                     NavigationExecutor navigationExecutor,
                                     Navigator navigator,
                                     PlayQueueHelper playQueueHelper,
                                     EventTracker eventTracker,
                                     PlaylistItemMenuRendererFactory playlistItemMenuRendererFactory,
                                     AccountOperations accountOperations,
                                     EntityItemCreator entityItemCreator,
                                     OfflineSettingsStorage offlineSettingsStorage,
                                     ChangeLikeToSaveExperiment changeLikeToSaveExperiment,
                                     FeedbackController feedbackController) {
        this.appContext = appContext;
        this.eventBus = eventBus;
        this.playlistRepository = playlistRepository;
        this.likeOperations = likeOperations;
        this.repostOperations = repostOperations;
        this.sharePresenter = sharePresenter;
        this.screenProvider = screenProvider;
        this.featureOperations = featureOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.navigationExecutor = navigationExecutor;
        this.navigator = navigator;
        this.playQueueHelper = playQueueHelper;
        this.eventTracker = eventTracker;
        this.playlistItemMenuRendererFactory = playlistItemMenuRendererFactory;
        this.accountOperations = accountOperations;
        this.entityItemCreator = entityItemCreator;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
        this.feedbackController = feedbackController;
    }

    public void show(View button, PlaylistItem playlist) {
        show(button, playlist, null, ItemMenuOptions.Companion.createDefault());
    }

    public void show(View button, Urn playlistUrn) {
        if (!isShowing) {
            this.eventContextMetadataBuilder = Optional.absent();
            this.renderer = playlistItemMenuRendererFactory.create(this, button);
            this.playlistUrn = playlistUrn;
            this.promotedSourceInfo = Optional.absent();
            this.entityMetadata = EntityMetadata.EMPTY;
            this.itemMenuOptions = ItemMenuOptions.Companion.createDefault();
            loadPlaylist(playlistUrn);
            isShowing = true;
        }
    }

    public void show(View button,
                     PlaylistItem playlist,
                     EventContextMetadata.Builder eventContextMetadataBuilder,
                     ItemMenuOptions itemMenuOptions) {
        if (!isShowing) {
            this.eventContextMetadataBuilder = Optional.fromNullable(eventContextMetadataBuilder);
            renderer = playlistItemMenuRendererFactory.create(this, button);
            playlistUrn = playlist.getUrn();
            promotedSourceInfo = loadPromotedSourceInfo(playlist);
            entityMetadata = EntityMetadata.from(playlist);
            this.itemMenuOptions = itemMenuOptions;
            loadPlaylist(playlistUrn);
            isShowing = true;
        }
    }

    private Optional<PromotedSourceInfo> loadPromotedSourceInfo(PlaylistItem playlist) {
        if (playlist.isPromoted()) {
            return Optional.of(PromotedSourceInfo.fromItem(playlist));
        }
        return Optional.absent();
    }

    @Override
    public void onDismiss() {
        playlistDisposable.dispose();
        playlistDisposable = Disposables.empty();
        isShowing = false;
    }

    @Override
    public void handleGoToArtistProfile(Urn creatorUrn) {
        navigator.navigateTo(NavigationTarget.forProfile(creatorUrn));
    }

    public void handlePlayNext() {
        playQueueHelper.playNext(playlistUrn);
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayNext(playlistUrn,
                                                                   screenProvider.getLastScreenTag(),
                                                                   getEventContextMetadata()));
    }

    private static FragmentManager toFragmentManager(Context context) {
        return getFragmentActivity(context).getSupportFragmentManager();
    }

    public void saveOffline(Context context, PlaylistItem playlist) {
        if (offlineSettingsStorage.isOfflineContentAccessible()) {
            handleSaveOffline(playlist);
        } else {
            OfflineStorageErrorDialog.show(toFragmentManager(context));
        }
    }

    private void handleSaveOffline(PlaylistItem playlist) {
        if (playlist.isUserLike() || isPlaylistOwnedByCurrentUser(playlist)) {
            saveOffline();
        } else {
            likeAndSaveOffline();
        }

        eventBus.publish(EventQueue.TRACKING, OfflineInteractionEvent.fromAddOfflinePlaylist(
                screenProvider.getLastScreenTag(),
                playlistUrn,
                promotedSourceInfo.orNull()));
    }

    public void removeFromOffline(Context context) {
        if (offlineContentOperations.isOfflineCollectionEnabled()) {
            ConfirmRemoveOfflineDialogFragment.showForPlaylist(toFragmentManager(context),
                                                               playlistUrn,
                                                               promotedSourceInfo.orNull());
        } else {
            offlineContentOperations.makePlaylistUnavailableOffline(playlistUrn).subscribe(new DefaultCompletableObserver());
            eventBus.publish(EventQueue.TRACKING, OfflineInteractionEvent.fromRemoveOfflinePlaylist(
                    screenProvider.getLastScreenTag(),
                    playlistUrn,
                    promotedSourceInfo.orNull()));
        }
    }

    public void deletePlaylist(Context context) {
        DeletePlaylistDialogFragment.show(toFragmentManager(context), playlistUrn);
    }

    private EventContextMetadata getEventContextMetadata() {
        final EventContextMetadata.Builder builder = eventContextMetadataBuilder.isPresent() ?
                                                     eventContextMetadataBuilder.get() :
                                                     EventContextMetadata.builder()
                                                                         .pageName(screenProvider.getLastScreenTag());

        return builder.isFromOverflow(true).build();
    }

    public void handleLike(PlaylistItem playlist) {
        boolean addLike = !playlist.isUserLike();
        likeOperations.toggleLike(playlistUrn, addLike)
                      .observeOn(AndroidSchedulers.mainThread())
                      .toCompletable()
                      .subscribe(new LikeToggleObserver(appContext, addLike, changeLikeToSaveExperiment, feedbackController, navigationExecutor));

        eventTracker.trackEngagement(
                UIEvent.fromToggleLike(addLike,
                                       playlistUrn,
                                       getEventContextMetadata(),
                                       promotedSourceInfo.orNull(),
                                       entityMetadata));

        if (isUnlikingNotOwnedPlaylistInOfflineMode(addLike, playlist)) {
            offlineContentOperations.makePlaylistUnavailableOffline(playlistUrn).subscribe(new DefaultCompletableObserver());
        }
    }

    public void handleRepost(boolean addRepost) {
        repostOperations.toggleRepost(playlistUrn, addRepost)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RepostResultSingleObserver(appContext));

        eventTracker.trackEngagement(
                UIEvent.fromToggleRepost(addRepost,
                                         playlistUrn,
                                         getEventContextMetadata(),
                                         promotedSourceInfo.orNull(),
                                         entityMetadata));
    }

    @Override
    public void handleUpsell(Context context) {
        navigationExecutor.openUpgrade(context, UpsellContext.OFFLINE);
        eventBus.publish(EventQueue.TRACKING,
                         UpgradeFunnelEvent.forPlaylistItemClick(screenProvider.getLastScreenTag(),
                                                                 playlistUrn));
    }

    public void handleShare(Context context, PlaylistItem playlist) {
        final boolean isPublic = !playlist.isPrivate();

        if (isPublic) {
            sharePresenter.share(context,
                                 playlist.permalinkUrl(),
                                 getEventContextMetadata(), promotedSourceInfo.orNull(),
                                 EntityMetadata.from(playlist));
        }
    }

    private void likeAndSaveOffline() {
        final boolean addLike = true;
        likeOperations.toggleLike(playlistUrn, addLike)
                      .observeOn(AndroidSchedulers.mainThread())
                      .toCompletable()
                      .doOnComplete(this::saveOffline)
                      .subscribe(new LikeToggleObserver(appContext, addLike, changeLikeToSaveExperiment, feedbackController, navigationExecutor));
    }

    private void saveOffline() {
        offlineContentOperations.makePlaylistAvailableOffline(playlistUrn).subscribe(new DefaultCompletableObserver());
    }

    private boolean isUnlikingNotOwnedPlaylistInOfflineMode(boolean addLike, PlaylistItem playlist) {
        boolean offlineContentEnabled = featureOperations.isOfflineContentEnabled();
        return offlineContentEnabled && !addLike && !isPlaylistOwnedByCurrentUser(playlist);
    }

    private boolean isPlaylistOwnedByCurrentUser(PlaylistItem playlist) {
        return accountOperations.isLoggedInUser(playlist.creatorUrn());
    }

    // this is really ugly. We should introduce a PlaylistRepository.
    // https://github.com/soundcloud/android-listeners/issues/2942
    private void loadPlaylist(Urn urn) {
        playlistDisposable.dispose();
        playlistDisposable = playlistRepository.withUrn(urn)
                                               .observeOn(AndroidSchedulers.mainThread())
                                               .subscribeWith(new PlaylistSubscriber());
    }

    private final class PlaylistSubscriber extends DefaultMaybeObserver<Playlist> {

        @Override
        public void onSuccess(Playlist playlist) {
            renderer.render(entityItemCreator.playlistItem(playlist), itemMenuOptions);
        }
    }
}
